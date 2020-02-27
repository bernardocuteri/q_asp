%@forall
a | -a.
b | -b.
%@exists
c:- a.
c:- b.
c:- d.
%@constraint
:- not c.

