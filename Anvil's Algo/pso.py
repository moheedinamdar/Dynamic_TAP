#/usr/bin/env python3

#					PSO algo
#
# Initialize the particle's position with a uniformly distributed random vector: xi ~ U(blo, bup)
# Initialize the particle's best known position to its initial position: pi ← xi
# if f(pi) < f(g) then
#     update the swarm's best known  position: g ← pi
# Initialize the particle's velocity: vi ~ U(-|bup-blo|, |bup-blo|)
# while a termination criterion is not met do:
#   for each dimension d = 1, ..., n do
#      Pick random numbers: rp, rg ~ U(0,1)
#      Update the particle's velocity: vi,d ← ω vi,d + φp rp (pi,d-xi,d) + φg rg (gd-xi,d)
#      Update the particle's position: xi ← xi + vi
#      if f(xi) < f(pi) then
#         Update the particle's best known position: pi ← xi
#      if f(pi) < f(g) then
#         Update the swarm's best known position: g ← pi
#

import HB as nearby
import GDMAPI as Maps
connected = nearby.Get_Count()

state = []
last_state = []
my_best_state = []
our_best_state = []

decision = []
last_decision = []

def compare(to):
    """Function for comparing states.
    Belongs to the state object.
    """
    difference = 0
    for i in range(0,connected):
	difference += state[i] - to[i]

    return difference

def init_state():
    state = Maps.fetch()

def Set_Best():
    """Uses HB Module to pull the group best,
    Then it builds the constants for this PSO round.
    """
    if (state is None):
	init_state()
	last_decision = None
    return 

    our_best_state = nearby.Get_Best()
    if (state.compare(my_best_state) < 0):	#state is better than my_best
	my_best_state = state

    return


def PSO_step():
    """Performs PSO step for this cycle.
    One iteration of PSO. Can be called again for more steps.
    """

    from random import random
    r1 = random()
    r2 = random()
    for i in range(0,connected):	
        if last_decision != None:
	    decision[i] = a*last_decision[i] + b*r1*(my_best_state[i] - state[i]) + c*r2*(our_best_state[i] - state[i])
	    state[i] = (state[i] + decision[i])/(decision_max-decision_min)	#scaling it back to [0,1]

