import matplotlib
import matplotlib.pyplot as plt
from datetime import datetime

# fig, axs = plt.subplots(nrows=4, ncols=6, figsize=(17, 10), sharey=True, sharex=True)
fig, axs = plt.subplots(nrows=2, ncols=2, figsize=(16, 16), sharey=True, sharex=True)

places = ["Africa", "Europe"]
models = ["Mesh", "Extreme"]

for p, place in enumerate(places):
    for m, model in enumerate(models):

        file_name = place + "_" + model

        lines = []
        with open("./paper_results/" + file_name + ".log") as f:
            lines = f.readlines()
        lines = [line[:-1] for line in lines]

        time_stamp_0 = None
        time_stamps_anal = []
        time_stamps_solu = []
        solu_sats_total = []
        solu_planes = []
        solu_incl = []
        solu_mcg = []
        for line in lines:
            fields = line.split(' ')

            if fields[0][0:2] != '20':
                # HEADER/TAIL
                pass
            else:
                # DATA
                # 0: date
                if time_stamp_0 == None:
                    time_stamp_0 = datetime.strptime(fields[0].split('.')[0], '%Y-%m-%dT%H:%M:%S')
                time_stamp = datetime.strptime(fields[0].split('.')[0], '%Y-%m-%dT%H:%M:%S')
                # 2: 'Analyzing:', 'Discarded:', 'SOLUTION!:'
                if fields[2] == 'Analyzing:':
                    time_stamps_anal.append((time_stamp-time_stamp_0).total_seconds())
                elif fields[2] == 'Discarded:':
                    print('Discarded')
                elif fields[2] == 'SOLUTION!:':
                    time_stamps_solu.append((time_stamp-time_stamp_0).total_seconds()/60)
                    solu_planes.append(int(fields[3]))
                    solu_sats_total.append(int(fields[6])*int(fields[3]))
                    solu_incl.append(float(fields[9]))
                    solu_mcg.append(float(fields[12]))
                    print('SOLUTION: planes={}, sats={}, inc={}, msg={}'.format(fields[3], fields[6], fields[9], fields[12]))
                    # 3: planes, 6: sats, 9: inc1 
                else:
                    assert False


        axs[m][p].grid(axis='x', which='both', color='0.95')
        axs[m][p].grid(axis='y', which='major', color='0.93')
        axs[m][p].grid(axis='y', which='minor', color='0.96')

        axs[m][p].set_xlabel("Computation time stamp [min]")
        axs[m][p].set_ylabel("Total satellites in costellation [#]")

        axs[m][p].set_title(place + " " + model)
        axs[m][p].scatter(time_stamps_solu, solu_sats_total, zorder=10, color='orange', marker="o", s=8, label="Solution")
        for t, time in enumerate(time_stamps_solu):
            axs[m][p].annotate("p:{} i:{:.1f}\nmcg:{:.1f}".format(solu_planes[t], solu_incl[t], solu_mcg[t]), (time, solu_sats_total[t] + 0.5), ha='center')

plt.savefig("./paper_results/" + "plots.png", format='png', dpi=300)
plt.savefig("./paper_results/" + "plots.pdf", format='pdf')
