import matplotlib
import matplotlib.pyplot as plt
from datetime import datetime

# maximum contact gap (mcg)
# const. size   rect=larg, mesh=low  (sats/planes)
# inclination   rect=high, mesh=low

# compl. lvls   rect=high, mesh=few
# memory/time   rect=low , mesh=high

fig, axs = plt.subplots(nrows=1, ncols=1, figsize=(5, 5))


###################
# Parse Logs
###################

file_name = "Global_NBIoT"

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
                print('Discarded')

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

    axs.grid(axis='x', which='both', color='0.95')
    axs.grid(axis='y', which='major', color='0.93')
    axs.grid(axis='y', which='minor', color='0.96')

    # axs.set_yscale('log')

    #axs.set_xlim([7, 38])
    #axs.set_ylim([5, 125])

    axs.axhline(y=186, color='gray', linestyle=':')
    axs.axhline(y=120, color='gray', linestyle=':')

    axs.set_title("Global Mesh")
    
    axs.set_xlabel("Total satellites in costellation [#]")
    axs.set_ylabel("Maximum Contact Gap [min]")

    axs.scatter(solu_sats_total, solu_mcg, zorder=10, c=solu_planes, marker="o", s=300, label="Solution")

    # Annotations
    for s, _ in enumerate(solu_sats_total):
        # planes
        if solu_planes[s] <= 4:
            color = 'white'
        else:
            color = 'black'
        axs.annotate("{}".format(solu_planes[s]), 
                            (solu_sats_total[s], solu_mcg[s]), color=color,
                            zorder=12, ha='center', va='center')

        # inclination
        axs.annotate("i:{:.1f}".format(solu_incl[s]), 
                            (solu_sats_total[s], solu_mcg[s] + 5), 
                            zorder=12, ha='center', va='center')

plt.tight_layout()

plt.savefig("./paper_results/" + "plots_perf_global_nbiot.png", format='png', dpi=300)
plt.savefig("./paper_results/" + "plots_perf_global_nbiot.pdf", format='pdf')
