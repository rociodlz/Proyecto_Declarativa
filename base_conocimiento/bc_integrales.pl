:- consult('bc_derivadas.pl').
:- consult('bc_simplificaciones_algebraicas.pl').


integral(F, X, R) :-
    atom(X),
    once(i(F, X, RI)),
    simplificar_algebra(RI, R).


i(C, X, C*X) :-
    number(C).

i(F, X, X^2/2) :-
    F == X.

i(C*F, X, C*(R)) :-
    number(C),
    i(F, X, R).

i(F^C, X, X^(C+1)/(C+1)) :-
    F == X,
    integer(C),
    C =\= -1.

i(X^(-1), X, ln(abs(X))) :- !.



i(u(X), X, pendiente(u(X), X)).

i(v(X), X, pendiente(v(X), X)).



i(F+G, X, (RF)+(RG)) :-
    i(F, X, RF),
    i(G, X, RG).

i(F-G, X, (RF)-(RG)) :-
    i(F, X, RF),
    i(G, X, RG).

i(C^F, X, C^X/ln(C)) :-
    number(C),
    C > 0,
    C =\= 1,
    F == X.

i(C^u(X), X, pendiente(C^u(X), X)) :-
    number(C),
    C > 0,
    C =\= 1.




% TRIGONOMETRICAS

i(sin(X), X, -cos(X)) :- !.

i(cos(X), X, sin(X)) :- !.

i(tan(X), X, -ln(abs(cos(X)))) :- !.


i(asin(X), X, X*asin(X)+raiz(1-X^2,2)) :- !.

i(acos(X), X, X*acos(X)-raiz(1-X^2,2)) :- !.

i(atan(X), X, X*atan(X)-ln(1+X^2)/2) :- !.



% HIPERBOLICAS

i(sinh(X), X, cosh(X)) :- !.

i(cosh(X), X, sinh(X)) :- !.

i(tanh(X), X, ln(cosh(X))) :- !.

i(asinh(X), X, X*asinh(X)-raiz(X^2+1,2)) :- !.

i(acosh(X), X, X*acosh(X)-raiz(X-1,2)*raiz(X+1,2)) :- !.

i(atanh(X), X, X*atanh(X)+ln(1-X^2)/2) :- !.
