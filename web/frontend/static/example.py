import os
import logging
from datetime import datetime

# If grpc is missing: pip install grpcio
from google.protobuf import empty_pb2
import grpc

# If the classes below are missing, generate them:
# You need to install the grpcio-tools to generate the stubs: pip install grpcio-tools
# python -m grpc_tools.protoc -I . --python_out=. --grpc_python_out=. challenger.proto
import challenger_pb2 as ch
import challenger_pb2_grpc as api

op = [('grpc.max_send_message_length', 10 * 1024 * 1024),
      ('grpc.max_receive_message_length', 100 * 1024 * 1024)]
with grpc.insecure_channel('challenge.msrg.in.tum.de:5023', options=op) as channel:
    stub = api.ChallengerStub(channel)


    #Step 1 - Create a new Benchmark
    benchmarkconfiguration = ch.BenchmarkConfiguration(
        token=os.environ['API_TOKEN'].strip(), #The api token is available in the profile, see here: https://challenge.msrg.in.tum.de/profile/
        benchmark_name="shows_up_in_dashboard", #This name is used here: https://challenge.msrg.in.tum.de/benchmarks/
        benchmark_type="test", #Test or Evaluation, Evaluation will be available end of January. Test can be used to start implementing
        queries=[ch.Query.Q1, ch.Query.Q2])
    benchmark = stub.createNewBenchmark(benchmarkconfiguration)

    #Step 2 - Start Eventprocessing and start the clock
    stub.startBenchmark(benchmark)

    event_count = 0

    # Step 3 - start event processing
    while True:
        batch = stub.nextBatch(benchmark)
        event_count = event_count + len(batch.events)

        def queryResults(symbols:list[str]) -> list[ch.Indicator]:
            # Your part: calculate the indicators for the given symbols
            return list()

        resultQ1 = ch.ResultQ1(benchmark_id=benchmark.id, #The id of the benchmark
                            batch_seq_id=batch.seq_id, #The sequence id of the batch
                            indicators=queryResults(batch.lookup_symbols))
        stub.resultQ1(resultQ1)  # send the result of query 1 back

        def crossoverEvents() -> list[ch.CrossoverEvent]:
            #Your part: calculate the crossover events
            return list()

        # do the same for Q2
        resultQ2 = ch.ResultQ2(benchmark_id=benchmark.id, #The id of the benchmark
                            batch_seq_id=batch.seq_id, #The sequence id of the batch
                            crossover_events=crossoverEvents()) 
        stub.resultQ2(resultQ2)

        # Step 4 - once the last event is received, stop the clock
        # See the statistics within ~5min here: https://challenge.msrg.in.tum.de/benchmarks/
        if batch.last:
            print(f"received last batch, total batches: {event_count}")
            stub.endBenchmark(benchmark)
            break
