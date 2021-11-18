import matplotlib
import matplotlib.pyplot as plt
from datetime import datetime

places = ["Africa", "Europe"]
models = ["Mesh_60", "Mesh", "Mesh_NBIoT"]
max_mcg = [60, 120, 186]

africa_min_sats = [0,0,0]
africa_min_sats_incl = [0,0,0]
africa_min_sats_planes = [0,0,0]
europe_min_sats = [0,0,0]
europe_min_sats_incl = [0,0,0]
europe_min_sats_planes = [0,0,0]

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

        if model == "Mesh":
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

        ###################
        # Plot Solutions
        ###################

        min_sats = min(solu_sats_total)
        min_sats_index = solu_sats_total.index(min_sats)
        min_sats_mcg = solu_mcg[min_sats_index]
        min_sats_planes = solu_planes[min_sats_index]
        min_sats_incl = solu_incl[min_sats_index]

        if place == "Africa": 
            africa_min_sats[m] = min_sats
            africa_min_sats_incl[m] = min_sats_incl
            africa_min_sats_planes[m] = min_sats_planes
        
        if place == "Europe": 
            europe_min_sats[m] = min_sats
            europe_min_sats_incl[m] = min_sats_incl
            europe_min_sats_planes[m] = min_sats_planes


print(africa_min_sats)
print(europe_min_sats)

fig, axs = plt.subplots(nrows=1, ncols=1, figsize=(5, 5))

axs.set_title("Africa and Europe Mesh")

axs.grid(axis='x', which='both', color='0.95')
axs.grid(axis='y', which='major', color='0.93')
axs.grid(axis='y', which='minor', color='0.96')

#axs[p+2*m].set_xlim([7, 38])
#axs[p+2*m].set_ylim([0, 10.5])

axs.set_xlabel(r'$g_{max}$')
axs.set_ylabel(r"Minuimum Fleet Size ($\mathcal{S}$)")

dim = 0.75
dimw = 0.75/2 + 0.05

color_africa = "#fde725"
color_europe = "#79cfa6"

axs.bar(0 - dimw/2, africa_min_sats[0], dimw, color=color_africa, zorder=10)
axs.bar(0 + dimw/2, europe_min_sats[0], dimw, color=color_europe, zorder=10)

axs.bar(1 - dimw/2, africa_min_sats[1], dimw, color=color_africa, zorder=10)
axs.bar(1 + dimw/2, europe_min_sats[1], dimw, color=color_europe, zorder=10)

axs.bar(2 - dimw/2, africa_min_sats[2], dimw, color=color_africa, zorder=10)
axs.bar(2 + dimw/2, europe_min_sats[2], dimw, color=color_europe, zorder=10)

# Annotations
for m in [0,1,2]:

    # inclination
    axs.annotate("incl:\n{:.1f}".format(africa_min_sats_incl[m]), 
                        (m - dimw/2, europe_min_sats[m] - 0.8), 
                        zorder=12, ha='center', va='center')
    axs.annotate("incl:\n{:.1f}".format(europe_min_sats_incl[m]), 
                        (m + dimw/2, europe_min_sats[m] - 0.8), 
                        zorder=12, ha='center', va='center')

    # planes
    axs.annotate("planes:\n{}".format(africa_min_sats_planes[m]), 
                        (m - dimw/2, europe_min_sats[m] - 2), 
                        zorder=12, ha='center', va='center')
    axs.annotate("planes:\n{}".format(europe_min_sats_planes[m]), 
                        (m + dimw/2, europe_min_sats[m] - 2), 
                        zorder=12, ha='center', va='center')


#axs.set_xticklabels(["60", "120", "186"])
axs.set_xticks([0,1,2])
axs.set_xticklabels(["60", "120", "186"])

colors = {'Africa':color_africa, 'Europe':color_europe}         
labels = list(colors.keys())
handles = [plt.Rectangle((0,0),1,1, color=colors[label]) for label in labels]
plt.legend(handles, labels)

plt.tight_layout()

plt.savefig("./paper_results/" + "plots_var_mcg.png", format='png', dpi=300)
plt.savefig("./paper_results/" + "plots_var_mcg.pdf", format='pdf')

