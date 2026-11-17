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

% Negación: ∫ -f dx = -∫ f dx. Es c) y f) con k = -1: -x = (-1)*x. Prolog lee
% "-x" como -(x), no como un producto, así que la regla de C*F no la cubre.
i(-F, X, -(R)) :-
    i(F, X, R).

i(C^F, X, C^X/ln(C)) :-
    number(C),
    C > 0,
    C =\= 1,
    F == X.

% a^x con a = e (número de Euler). En Prolog "e" es un átomo, no un número, así
% que la regla anterior (number(C)) no la cubre. Como ln(e) = 1, la primitiva es
% la misma función.
i(e^F, X, e^X) :-
    F == X, !.

i(exp(F), X, exp(X)) :-
    F == X, !.

% j) a^u con u lineal en X (u' constante distinta de 0):
%     ∫ a^u dx = a^u / (u' * ln(a))      ∫ e^u dx = e^u / u'
% Si u no es lineal (por ejemplo x^2), la primitiva no es elemental y no hay regla.
i(C^U, X, C^U/(M*ln(C))) :-
    number(C),
    C > 0,
    C =\= 1,
    derivada_constante(U, X, M), !.

i(e^U, X, e^U/M) :-
    derivada_constante(U, X, M), !.

i(exp(U), X, exp(U)/M) :-
    derivada_constante(U, X, M), !.

%   derivada_constante(+U, +X, -M)  U depende de X y su derivada es el número M =\= 0.
derivada_constante(U, X, 1) :-
    U == X, !.
derivada_constante(-U, X, M) :- !,
    derivada_constante(U, X, M0),
    M is -M0.
derivada_constante(U, X, M) :-
    U \== X,
    sub_term(S, U), S == X, !,
    catch(d(U, X, D), _, fail),
    \+ ( sub_term(S2, D), S2 == X ),
    catch(M is D, _, fail),
    M =\= 0.

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
