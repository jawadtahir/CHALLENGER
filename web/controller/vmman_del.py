import os
import subprocess

from vmman_create import TEMPLATE_DIR

DEL_SCRIPT_TEMPLATE_FILE = os.path.join(TEMPLATE_DIR, "vm_del_template.sh")
DEL_SCRIPT_FILE = "vm_del.sh"

def vm_delete(team_name: str)->None:
    
    script_text = ""
    with open(DEL_SCRIPT_TEMPLATE_FILE) as template:
        script_text = "".join(template.readlines())
    
    script_text = script_text.format(team=team_name)

    del_script_path = os.path.join(team_name,DEL_SCRIPT_FILE)
    with open(del_script_path, "w") as script:
        script.write(script_text)

    os.chmod(del_script_path, 0o764)

    sh_proc = subprocess.Popen("./"+DEL_SCRIPT_FILE, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, cwd=team_name, text=True, shell=True)
    
    while sh_proc.poll() is None:
        print(sh_proc.stdout.readline())




if __name__ == "__main__":
    vm_delete("group1")