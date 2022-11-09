import asyncio
import logging
import copy
from logging.config import dictConfig
# from quart.logging import serving_handler

import paramiko
import os
import traceback
from signal import SIGTERM, SIGINT
from typing import Any, Callable, Awaitable

from hypercorn.asyncio import serve
from hypercorn.config import Config
from quart import Quart, websocket, render_template, redirect, url_for, request, flash, make_response
from quart_auth import AuthManager, login_required, Unauthorized, login_user, AuthUser, logout_user, current_user

from sshpubkeys import SSHKey, InvalidKeyError
import frontend.helper as helper
import frontend.worker as worker
from frontend.admin import hash_password
from frontend.models import db, ChallengeGroup, get_group_information, get_recent_changes, \
    get_benchmarks_by_group, get_benchmark, get_benchmarkresults, VirtualMachines, get_vms_of_group, get_querymetrics, \
    benchmark_get_is_active, get_evaluation_results
from shared.util import raise_shutdown, Shutdown

shutdown_event = asyncio.Event()
PRIVATE_KEY_PATH = os.environ.get("PRIVATE_KEY_PATH", "cochairs")


def signal_handler(*_: Any) -> None:
    shutdown_event.set()


dictConfig({
    'version': 1,
    'loggers': {
        'quart.app': {
            'level': 'INFO',
        },
        'gino.engine': {
            'level': 'WARNING',
        }
    },
})

# serving_handler.setFormatter(logging.Formatter('%(message)s'))

app = Quart(__name__)
app.secret_key = "-9jMkQIvmU2dksWTtpih2w"
AuthManager(app)


# logging.basicConfig(level=logging.INFO)

async def lastUpdate():
    #wait flash("Last FAQ update - March 29th", "info")
    return

@app.route('/')
async def index():
    if await current_user.is_authenticated:
        return redirect(url_for('profile'))
    else:
        return await render_template('index.html', name="Welcome!")


@app.route('/login/', methods=['GET', 'POST'])
async def login():
    app.logger.info("login")
    if request.method == 'POST':
        form = await request.form
        groupname = form['group'].strip()
        password = hash_password(form['password'].strip())

        group = await ChallengeGroup.query.where(
            ChallengeGroup.groupname == groupname and ChallengeGroup.password == password).gino.first()
        if group:
            login_success = True
            login_user(AuthUser(str(group.id)))
            if login_success:
                return redirect(url_for('profile'))
        else:
            return await render_template('login.html', name="Login", error='Invalid password')
    else:
        return await render_template('login.html', name="Login")


@app.route('/logout')
async def logout():
    app.logger.info("logout")
    logout_user()
    return redirect(url_for("index"))


@app.errorhandler(Unauthorized)
async def redirect_to_login(*_):
    return redirect(url_for("login"))


async def upload_pub_key(pubkey: str, vm_adrs: str, username, groupid, port: int = 22):
    app.logger.info("upload_pub_key")
    ssh = SSHKey(pubkey, strict=True)
    try:
        ssh.parse()
    except InvalidKeyError:
        await flash('Invalid key', "danger")
        print("Invalid key")
        traceback.print_exc()
        return
    except NotImplementedError:
        await flash('Invalid key type', "danger")
        print("Invalid key type")
        traceback.print_exc()
        return

    pkey = paramiko.RSAKey.from_private_key_file(PRIVATE_KEY_PATH)
    with paramiko.SSHClient() as client:
        client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        try:
            client.connect(vm_adrs, port, username, pkey=pkey, timeout=3.0)  # three seconds timeout
        except:
            print("Could not connect")
            return await flash("Could not connect to VM to set the key", "danger")

        with client.open_sftp() as sftpclient:
            try:
                filepath = '.ssh/authorized_keys'
                print("accessing authorized_keys")
                with sftpclient.open(filepath) as authorized_keys:
                    print("opened file")
                    for authorized in authorized_keys:
                        if pubkey in authorized:
                            print("key already added")
                            return await flash("Key already added", "danger")
            except IOError:
                traceback.print_exc()
                print('authorized does not exist, continue')  # file does not exist, also ok

        try:
            client.exec_command('mkdir -p ~/.ssh/', timeout=3.0)
            client.exec_command('echo "%s" >> ~/.ssh/authorized_keys' % pubkey, timeout=3.0)
            client.exec_command('chmod 644 ~/.ssh/authorized_keys', timeout=3.0)
            client.exec_command('chmod 700 ~/.ssh/', timeout=3.0)
            await flash("Key successful added", "success")
        except:
            print("Error while setting ssh key")
            traceback.print_exc()
            return await flash("Error while setting ssh key", "danger")

    vms = await VirtualMachines.query.where(VirtualMachines.group_id == groupid).gino.all()
    for vm in vms:
        if vm.internaladrs == vm_adrs or (vm_adrs in vm.forwardingadrs):
            await vm.update(sshpubkey=pubkey).apply()


@app.route('/profile/', methods=['GET', 'POST'])
@login_required
async def profile():
    await lastUpdate()

    app.logger.info("profile")
    if request.method == 'POST':
        group = await get_group_information(current_user.auth_id)
        form = await request.form

        if 'profile' in form:
            print("profile")
            groupnick = form['groupnick'].strip()
            groupemail = form['groupemail'].strip()

            if len(groupnick) > 32:
                await flash('Nickname should be below 32 chars', "danger")
                return redirect(url_for('profile'))

            await group.update(groupnick=groupnick, groupemail=groupemail).apply()
            await flash('Profile saved', "success")

            return redirect(url_for('profile'))
    else:
        group = await get_group_information(current_user.auth_id)
        if group is None:
            return redirect(url_for("logout"))
        vms = await get_vms_of_group(group.id)
        return await render_template('profile.html', name="Profile", group=group, vms=vms,
                                     menu=helper.menu(profile=True))


@app.route('/vms/', methods=['GET', 'POST'])
@login_required
async def vms():
    await lastUpdate()

    app.logger.info("vms")
    group = await get_group_information(current_user.auth_id)

    if request.method == 'POST':
        form = await request.form
        print("sshkey")
        err = False
        if 'VMAdrs' not in form:
            await flash('No VM selected', "danger")
            err = True
        if 'sshpubkey' not in form or len(form['sshpubkey'].strip()) <= 30:
            await flash('No sshpubkey or invalid sshpubkey added', "danger")
            err = True

        if err:
            return redirect(url_for('profile'))

        vmadrs = form['VMAdrs'].strip()
        sshkey = form['sshpubkey'].strip()
        groupname = group.groupname
        vmadrs = vmadrs.split("/")[1]
        try:
            await upload_pub_key(sshkey, vmadrs, groupname, group.id, 22)
        except Exception as e:
            print(e)
            print(traceback.format_exc())
            await flash(
                "Error connecting to VM. Please inform the challenge organizers, debsgc22@gmail.com", "danger")

        return redirect(url_for('vms'))
    else:
        vms = await get_vms_of_group(group.id)
        ssh = {}
        for vm in vms:
            splitted = vm.forwardingadrs.split(':')
            ssh[vm.id] = "ssh -p %s %s@%s" % (splitted[1], group.groupname, splitted[0])

        return await render_template('vms.html',
                                     vms=vms,
                                     ssh=ssh,
                                     name="Virtual machine",
                                     group=group,
                                     menu=helper.menu(vms=True))


@app.route('/faq/')
@login_required
async def faq():
    app.logger.info("faq")
    group = await get_group_information(current_user.auth_id)
    return await render_template('faq.html', name="FAQ", group=group,
                                 menu=helper.menu(faq=True))


@app.route('/documentation/')
@login_required
async def documentation():
    await lastUpdate()
    app.logger.info("documentation")
    group = await get_group_information(current_user.auth_id)
    return await render_template('documentation.html', name="Documentation", group=group,
                                 menu=helper.menu(documentation=True))


@app.route('/benchmarks/')
@login_required
async def benchmarks():
    await lastUpdate()
    app.logger.info("benchmarks")
    group = await get_group_information(current_user.auth_id)
    benchmarks = await get_benchmarks_by_group(group.id)
    return await render_template('benchmarks.html',
                                 name="Benchmarks",
                                 group=group,
                                 benchmarks=benchmarks,
                                 menu=helper.menu(benchmarks=True))


@app.route('/benchmarkdetails/<int:benchmarkid>/')
@login_required
async def benchmarkdetails(benchmarkid):
    app.logger.info("benchmarkdetails")
    benchmark = await get_benchmark(benchmarkid)
    benchmarkresults = await get_benchmarkresults(benchmarkid)
    if benchmark:
        group = await get_group_information(current_user.auth_id)
        if group.id == benchmark.group_id:
            return await render_template('benchmarkdetails.html',
                                         name="Benchmark",
                                         group=group,
                                         benchmark=benchmark,
                                         benchmarkresults=benchmarkresults,
                                         menu=helper.menu(benchmarks=True))

    return redirect_to_login()

@app.route('/deactivatebenchmark/', methods=['POST'])
@login_required
async def deactivatebenchmark():
    if request.method == 'POST':
        app.logger.info("deactivatebenchmark")
        form = await request.form
        benchmarkid = form["benchmarkid"]
        #TODO: ensure that benchmark is from own group
        group = await get_group_information(current_user.auth_id)
        benchmark = await benchmark_get_is_active(group.id, int(benchmarkid))
        if not benchmark:
            await flash("Benchmark not found!", "danger")
        else:
            await benchmark.update(is_active=False).apply()
            await flash("Benchmark %s deactivated" % benchmarkid, "success")

        return redirect("/benchmarkdetails/%s" % (form["benchmarkid"]))
    else:
        return redirect('/profile')

@app.route('/querymetrics/<int:benchmarkid>/')
@login_required
async def querymetrics(benchmarkid):
    app.logger.info("querymetrics")

    async def generate():
        yield b'benchmark_id, batch_id, starttime, q1resulttime, q1latency, q2resulttime, q2latency\n'
        qm = await get_querymetrics(benchmarkid)
        for m in qm:
            yield b"%d, %d, %d, %d, %d, %d, %d\n" % (m.benchmark_id, m.batch_id, m.starttime, m.q1resulttime, m.q1latency, m.q2resulttime, m.q2latency)

    #return generate()
    return await make_response(generate(), {'Content-Type': 'text/csv',
                                            'Cache-Control': 'no-cache',
                                            'Transfer-Encoding': 'chunked',
                                            'Content-Disposition': 'attachment; filename=\"benchmark-%s-querymetrics.csv\"' % (benchmarkid)})


@app.route('/rawdata/')
@login_required
async def rawdata():
    app.logger.info("rawdata")
    group = await get_group_information(current_user.auth_id)
    d = os.environ["DATASET_DIR"]
    files = os.listdir(d)
    filesandsize = map(lambda f: [f, (os.path.getsize(os.path.join(d, f)) / (1024 * 1024))], files)

    return await render_template('rawdata.html', name="Rawdata", group=group, files=filesandsize,
                                 menu=helper.menu(rawdata=True))


@app.route('/recentchanges/')
async def recentchanges():
    app.logger.info("recentchanges")
    changes = await get_recent_changes()
    return await render_template('recentchanges.html', name="Recent changes", changes=changes,
                                 menu=helper.menu(recentchanges=True))


@app.route('/systemstatus')
@login_required
async def systemstatus():
    app.logger.info("systemstatus")
    return await render_template('systemstatus.html', menu=helper.menu(system_status=True), name="System status")


@app.route('/leaderboard')
@login_required
async def leaderboard():
    await lastUpdate()
    res = await get_evaluation_results()
    l = list()

    for a in res:
        l.append(dict(a._row))

    latencies = copy.deepcopy(l)
    latencies = sorted(latencies, key=lambda k: float(k['avg90percentile']))
    for i, lat in enumerate(latencies):
        lat["rank"] = i+1

    throughput = copy.deepcopy(l)
    throughput = sorted(throughput, key=lambda k: float(k['avgthroughput']), reverse=True)
    for i, lat in enumerate(throughput):
        lat["rank"] = i+1

    app.logger.info("leaderboard")

    return await render_template('leaderboard.html', latencies=latencies, throughput=throughput, menu=helper.menu(leaderboard=True), name="Leaderboard")


@app.route('/exampleresults')
@login_required
async def exampleresults():
    app.logger.info("exampleresults")
    return await render_template('exampleresults.html', menu=helper.menu(exampleresults=True), name="Example results")


@app.route('/feedback')
@login_required
async def feedback():
    app.logger.info("feedback")
    return await render_template('feedback.html', menu=helper.menu(feedback=True), name="Feedback")


@app.websocket('/ws')
@login_required
async def notifications():
    try:
        while True:
            await asyncio.sleep(1)
            await websocket.send('hello')
    except asyncio.CancelledError:
        # Handle disconnect here
        raise


@app.before_serving
async def db_connection():
    print("start db_connection")
    connection = os.environ['DB_CONNECTION']
    logging.debug("db-connection: {}".format(connection))
    await db.set_bind(connection)
    await db.gino.create_all()


def prepare_interactive_get_event_loop():
    asyncio.get_event_loop().run_until_complete(db_connection())
    return asyncio.get_event_loop()


async def mainloop(debug, loop):
    print("Run Debug Version of webserver")
    tasks = []
    # monitor_task = worker.process_server_monitor_metrics(loop, shutdown_event, os.environ['RABBIT_CONNECTION'])

    bind_adrs = os.environ.get("WEB_BIND", "localhost:8000")

    cfg = Config()
    # cfg.accesslog = True
    cfg.bind = [bind_adrs]

    if debug:
        cfg.debug = True
        print("starting with debugging enabled")
        cfg.use_reloader = True
        webserver_task = serve(app, cfg, shutdown_trigger=shutdown_event.wait)
    else:
        webserver_task = serve(app, cfg, shutdown_trigger=shutdown_event.wait)

    tasks.append(webserver_task)
    # tasks.append(monitor_task)

    # create the database connect, without starting doesn't make sense
    await db_connection()

    # await asyncio.gather(monitor_task, webserver_task, wakeup(shutdown_event))
    try:
        gathered_tasks = asyncio.gather(*tasks)
        await gathered_tasks
    except (Shutdown, KeyboardInterrupt):
        pass


def main():
    loop = asyncio.get_event_loop()

    loop.add_signal_handler(SIGTERM, signal_handler)
    loop.add_signal_handler(SIGINT, signal_handler)

    # loop.create_task(raise_shutdown(shutdown_event.wait, loop, "general"))

    loop.run_until_complete(mainloop(True, loop))


if __name__ == "__main__":
    main()
