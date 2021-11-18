import matplotlib
import matplotlib.pyplot as plt
from datetime import datetime

fig, axs = plt.subplots(nrows=1, ncols=4, figsize=(15, 5), sharey=True)

places = ["Africa", "Europe"]
models = ["Mesh_60", "Mesh", "Mesh_NBIoT"]
max_mcg = [60, 120, 183]

for p, place in enumerate(places):
    for m, model in enumerate(models):

        ###################
        # Parse Logs
        ###################

        file_name = place + "_" + model
        print(file_name)

        # Read lines
        lines = []
        with open("./paper_results/" + file_name + ".log") as f:
            lines = f.readlines()
        lines = [line[:-1] for line in lines]

        # Time stamps
        time_stamp_0 = None
        time_stamps_analizing = []
        time_stamps_solu = []

        # Analisis metrics
        analisis_complexity_lvl = []
        analisis_mcg = []
        analisis_compute_times_sec = []
        analisis_memory_usage_mb = []

        # Solution metrics
        solu_sats_total = []
        solu_planes = []
        solu_incl = []
        solu_mcg = []
        discarded = 0

        for line in lines:
            fields = line.split(' ')

            if fields[0][0:2] != '20': # Header or tail
                pass
            else: # Data

                # 0: Timestamp
                time_stamp = datetime.strptime(fields[0].split('.')[0], '%Y-%m-%dT%H:%M:%S')
                if time_stamp_0 == None: # Save first time stamp
                    time_stamp_0 = time_stamp
                
                # 2: 'Analyzing:', 'Discarded:', 'SOLUTION!:'

                if fields[2] == 'Analyzing:': # Collect compute time and memory usage
                    time_stamps_analizing.append((time_stamp-time_stamp_0).total_seconds())
                    
                    analisis_complexity_lvl.append(int(fields[13]))
                    analisis_mcg.append(float(fields[16]))
                    analisis_compute_times_sec.append(float(fields[20])/1000)
                    analisis_memory_usage_mb.append(float(fields[24]))

                elif fields[2] == 'Discarded:':
                    discarded += 1
                    # print('Discarded')

                elif fields[2] == 'SOLUTION!:':
                    time_stamps_solu.append((time_stamp-time_stamp_0).total_seconds()/60)
                    
                    solu_planes.append(int(fields[3]))
                    solu_sats_total.append(int(fields[6])*int(fields[3]))
                    solu_incl.append(float(fields[9]))
                    solu_mcg.append(float(fields[12]))
                    # print('SOLUTION: planes={}, sats={}, inc={}, mcg={}'.format(fields[3], fields[6], fields[9], fields[12]))
                    
                else:
                    assert False


