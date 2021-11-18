import matplotlib
import matplotlib.pyplot as plt
from datetime import datetime

fig, axs = plt.subplots(nrows=1, ncols=4, figsize=(15, 5), sharey=True)

places = ["Greenland", "SouthKorea", "Australia", "Brazil", "Lebanon", "Italy", "WorldMountains"]
model = "Mesh"

print("Region (metric), Total sats, Planes, Inclination")

for p, place in enumerate(places):

    ###################
    # Parse Logs
    ###################

    file_name = place + "_" + model
    # print(file_name)

    # Read lines
    lines = []
    with open("./paper_results/" + file_name + ".log") as f:
        lines = f.readlines()
    lines = [line[:-1] for line in lines]

    if model == "Mesh" and place in ["Greenland", "SouthKorea", "Brazil", "Australia", "WorldMountains"]:
        # Read lines from short
        lines2 = []
        with open("./paper_results/ShortConst/" + file_name + "_Short.log") as f:
            lines2 = f.readlines()
        lines2 = [line[:-1] for line in lines2]

        lines = lines + lines2

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

        if fields[0][0:3] != '202': # Header or tail
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

    ###################
    # Plot Solutions
    ###################

    min_sats = min(solu_sats_total)
    min_sats_index = solu_sats_total.index(min_sats)
    min_sats_mcg = solu_mcg[min_sats_index]
    
    min_sats_planes = solu_planes[min_sats_index]
    min_sats_incl = solu_incl[min_sats_index]

    print("{} (min sats.),{},{},{},{:.1f}".format(place,min_sats,min_sats_planes,min_sats_incl,min_sats_mcg))

    min_incl = min(solu_incl)
    min_incl_index = solu_incl.index(min_incl)
    min_incl_mcg = solu_mcg[min_incl_index]
    
    min_incl_planes = solu_planes[min_incl_index]
    min_incl_sats = solu_sats_total[min_incl_index]

    print("{} (min incl.),{},{},{},{:.1f}".format(place,min_incl_sats,min_incl_planes,min_incl,min_incl_mcg))


