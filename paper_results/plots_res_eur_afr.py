import matplotlib
import matplotlib.pyplot as plt
from datetime import datetime
from statistics import mean

# maximum contact gap (mcg)
# const. size   rect=larg, mesh=low  (sats/planes)
# inclination   rect=high, mesh=low

# compl. lvls   rect=high, mesh=few
# memory/time   rect=low , mesh=high

fig, axs = plt.subplots(nrows=1, ncols=4, figsize=(15, 5), sharey=True)

places = ["Africa", "Europe"]
models = ["Extreme", "Mesh"]

axes_to_join = []

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
        # Prepare Data
        ################### 

        compute_time_hrs_cl0 = 0
        compute_time_hrs_cl1 = 0
        compute_time_hrs_cl2 = 0
        compute_time_hrs_cl3 = 0
        compute_time_hrs_cl4 = 0

        memory_mb_cl0 = []
        memory_mb_cl1 = []
        memory_mb_cl2 = []
        memory_mb_cl3 = []
        memory_mb_cl4 = []

        for a, cl in enumerate(analisis_complexity_lvl):
            if cl == 0:
                compute_time_hrs_cl0 += analisis_compute_times_sec[a]/(60*60)
                memory_mb_cl0.append(analisis_memory_usage_mb[a])
            if cl == 1:
                compute_time_hrs_cl1 += analisis_compute_times_sec[a]/(60*60)
                memory_mb_cl1.append(analisis_memory_usage_mb[a])
            if cl == 2:
                compute_time_hrs_cl2 += analisis_compute_times_sec[a]/(60*60)
                memory_mb_cl2.append(analisis_memory_usage_mb[a])
            if cl == 3:
                compute_time_hrs_cl3 += analisis_compute_times_sec[a]/(60*60)
                memory_mb_cl3.append(analisis_memory_usage_mb[a])
            if cl == 4:
                compute_time_hrs_cl4 += analisis_compute_times_sec[a]/(60*60)
                memory_mb_cl4.append(analisis_memory_usage_mb[a])

        avg_memory_mb_cl0 = mean(memory_mb_cl0)
        avg_memory_mb_cl1 = mean(memory_mb_cl1)
        avg_memory_mb_cl2 = mean(memory_mb_cl2)
        avg_memory_mb_cl3 = mean(memory_mb_cl3)
        if len(memory_mb_cl4) != 0:
            avg_memory_mb_cl4 = mean(memory_mb_cl4)
        else:
            avg_memory_mb_cl4 = 0

        ###################
        # Plot Solutions
        ################### 

        if model == "Extreme":
            axs[p+2*m].set_title(place + " Rect")
        else:
            axs[p+2*m].set_title(place + " Mesh")

        axs[p+2*m].grid(axis='x', which='both', color='0.95')
        axs[p+2*m].grid(axis='y', which='major', color='0.93')
        axs[p+2*m].grid(axis='y', which='minor', color='0.96')

        dim = 0.75
        dimw = 0.75/2

        # Twin1
        #axs[p+2*m].set_xlim([7, 38])
        axs[p+2*m].set_ylim([0, 11.5])

        axs[p+2*m].set_xlabel("Complexity level")
        axs[p+2*m].set_ylabel("Compute time [hrs]", color='blue')

        axs[p+2*m].bar(0 - dimw/2, compute_time_hrs_cl0, dimw, color='blue',  zorder=10),
        axs[p+2*m].bar(1 - dimw/2, compute_time_hrs_cl1, dimw, color='blue', zorder=10),
        axs[p+2*m].bar(2 - dimw/2, compute_time_hrs_cl2, dimw, color='blue', zorder=10),
        axs[p+2*m].bar(3 - dimw/2, compute_time_hrs_cl3, dimw, color='blue', zorder=10),
        axs[p+2*m].bar(4 - dimw/2, compute_time_hrs_cl4, dimw, color='blue', zorder=10),

        axs[p+2*m].annotate("Total compute time: {:.1f}hrs".format((compute_time_hrs_cl0 + compute_time_hrs_cl1 + compute_time_hrs_cl2 + compute_time_hrs_cl3 + compute_time_hrs_cl4)), 
                    (0, 0), xytext=(0.05, 0.95), textcoords=axs[p+2*m].transAxes,
                    zorder=12, ha='left', va='center', color='blue')  

        # Twin2
        axs2 = axs[p+2*m].twinx()

        # axs2.get_shared_y_axes().join(axs2, axs[3])

        axs2.set_ylim([0, 2400])
        axs2.set_ylabel("Average memory [MB]", color='purple')

        axs2.bar(0 + dimw/2, avg_memory_mb_cl0, dimw, color='purple', zorder=10),
        axs2.bar(1 + dimw/2, avg_memory_mb_cl1, dimw, color='purple', zorder=10),
        axs2.bar(2 + dimw/2, avg_memory_mb_cl2, dimw, color='purple', zorder=10),
        axs2.bar(3 + dimw/2, avg_memory_mb_cl3, dimw, color='purple', zorder=10),
        axs2.bar(4 + dimw/2, avg_memory_mb_cl4, dimw, color='purple', zorder=10),

        

        axs[p+2*m].annotate("Max memory usage: {:.1f}MB".format(max(avg_memory_mb_cl0, avg_memory_mb_cl1, avg_memory_mb_cl2, avg_memory_mb_cl3, avg_memory_mb_cl4)), 
                    (0, 0), xytext=(0.05, 0.9), textcoords=axs[p+2*m].transAxes,
                    zorder=12, ha='left', va='center', color='purple')  

        
        
plt.tight_layout()

plt.savefig("./paper_results/" + "plots_res_eur_afr.png", format='png', dpi=300)
plt.savefig("./paper_results/" + "plots_res_eur_afr.pdf", format='pdf')
