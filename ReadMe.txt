Total order broadcast is a fundamental communication primitive that plays a central role in
 bringing cheap software-based high availability to a wide range of services. 
 We present LCR, the first throughput optimal uniform total order broadcast protocol. LCR is
 based on a ring topology. It only relies on point-to-point inter-process communication and has a
 linear latency with respect to the number of processes. LCR is also fair in the sense that each
 process has an equal opportunity of having its messages delivered by all processes.
