import matplotlib
import matplotlib.pyplot as plt
from datetime import datetime

# maximum contact gap (mcg)
# const. size   rect=larg, mesh=low  (sats/planes)
# inclination   rect=high, mesh=low

# compl. lvls   rect=high, mesh=few
# memory/time   rect=low , mesh=high

fig, axs = plt.subplots(nrows=1, ncols=4, figsize=(15, 5), sharey=True)

places = ["Africa", "Europe"]
models = ["Extreme", "Mesh"]

for p, place in enumerate(places):
    for m, model in enumerate(models):

        ###################
        # Parse Logs
        ###################

        file_name = place + "_" + model

        # Read lines
        lines = []
        with open("./paper_results/" + file_name + ".log") as f:
            lines = f.readlines()
        lines = [line[:-1] for line in lines]

        #if model == "Mesh":
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

        axs[p+2*m].grid(axis='x', which='both', color='0.95')
        axs[p+2*m].grid(axis='y', which='major', color='0.93')
        axs[p+2*m].grid(axis='y', which='minor', color='0.96')

        # axs[p+2*m].set_yscale('log')

        axs[p+2*m].set_xlim([1, 38])
        axs[p+2*m].set_ylim([5, 125])

        axs[p+2*m].axhline(y=120, color='gray', linestyle=':')

        if model == "Extreme":
            axs[p+2*m].set_title(place + " Rect")
        else:
            axs[p+2*m].set_title(place + " Mesh")
        axs[p+2*m].set_xlabel("Total satellites in costellation [#]")
        axs[p+2*m].set_ylabel("Maximum Contact Gap [min]")

        axs[p+2*m].scatter(solu_sats_total, solu_mcg, zorder=10, c=solu_planes, marker="o", s=300, label="Solution")
        
        # Annotations
        for s, _ in enumerate(solu_sats_total):
            # planes
            if solu_planes[s] <= 4:
                color = 'white'
            else:
                color = 'black'
            axs[p+2*m].annotate("{}".format(solu_planes[s]), 
                              (solu_sats_total[s], solu_mcg[s]), color=color,
                              zorder=12, ha='center', va='center')

            # inclination
            axs[p+2*m].annotate("i:{:.1f}".format(solu_incl[s]), 
                              (solu_sats_total[s], solu_mcg[s] + 5), 
                              zorder=12, ha='center', va='center')

        # Tutorial
        if p == 0 and m == 0:
            s = 6
            axs[p+2*m].annotate("Number of planes", (solu_sats_total[s], solu_mcg[s] - 1),
                    xytext=(0.25, 0.42), textcoords=axs[p+2*m].transAxes,
                    zorder=12, ha='center', va='center',
                    arrowprops=dict(arrowstyle="->",
                        connectionstyle="arc, rad=1",
                        ls='-', lw=1))
            axs[p+2*m].annotate("Inclination", (solu_sats_total[s] + 2, solu_mcg[s] + 6),
                    xytext=(0.75, 0.6), textcoords=axs[p+2*m].transAxes,
                    zorder=12, ha='center', va='center',
                    arrowprops=dict(arrowstyle="->",
                        connectionstyle="arc, rad=1",
                        ls='-', lw=1))
            axs[p+2*m].annotate("Maximum contact\ngap threshold", (25, 120),
                    xytext=(0.75, 0.85), textcoords=axs[p+2*m].transAxes,
                    zorder=12, ha='center', va='center',
                    arrowprops=dict(arrowstyle="->",
                        connectionstyle="arc, rad=1",
                        ls='-', lw=1))

plt.tight_layout()

plt.savefig("./paper_results/" + "plots_perf_eur_afr.png", format='png', dpi=300)
plt.savefig("./paper_results/" + "plots_perf_eur_afr.pdf", format='pdf')
