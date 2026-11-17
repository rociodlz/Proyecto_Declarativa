:- consult('bc_simplificaciones.pl').


% Verificar si X es atomo o variable
variable_valida(X) :- atom(X).
variable_valida(X) :- var(X).

% Variable identificada para el calculo
variable(X) :- atom(X).


derivada(F, X, R):-
    d(F, X, DF),
    simplificar(DF, R).

d(C, _, 0):-
    number(C).

d(X, X, 1):-
    variable(X).

d(X^C, X, C*X^E):-
    variable(X),
    number(C),
    E is C-1,
    !.

d(C*X, X, C):-
    variable(X),
    number(C),
    !.



d(F + G, X, DF + DG):-
    variable(X),
    d(F, X, DF),
    d(G, X, DG),
    !.

d(F - G, X, DF - DG):-
    variable(X),
    d(F, X, DF),
    d(G, X, DG),
    !.



% Negacion unaria
d(-F, X, -DF) :-
    d(F, X, DF).

% POTENCIAS Y RAICES

d(F^C, X, C*F^(C-1)*(DF)) :-
    number(C),
    d(F, X, DF).    

d(raiz(F,C), X, (DF)/(C*F^((C-1)/C))) :-
    number(C),
    C > 0,
    d(F, X, DF).



% MULTIPLICACIONES

d(C*F, X, C*(DF)) :-
    number(C),
    d(F, X, DF).

d(F*G, X, (DF)*G + F*(DG)) :-
    d(F, X, DF),
    d(G, X, DG).



% DIVISIONES

d(F/C, X, (DF)/C) :-
    number(C),
    C =\= 0,
    d(F, X, DF).

d(C/F, X, -(C*(DF))/(F^2)) :-
    number(C),
    d(F, X, DF).

d(F/G, X, ((DF)*G-F*(DG))/(G^2)) :-
    d(F, X, DF),
    d(G, X, DG).



% EXPONENCIALES Y LOGARITMOS

d(e^F, X, e^F*(DF)) :-
    d(F, X, DF).

d(C^F, X, C^F*ln(C)*(DF)) :-
    number(C),
    C > 0,
    C =\= 1,
    d(F, X, DF).

d(exp(F), X, exp(F)*DF) :-
    d(F, X, DF).

d(ln(F), X, (DF)/F) :-
    d(F, X, DF).

d(log(C,F), X, (DF)/(F*ln(C))) :-
    number(C),
    C > 0,
    C =\= 1,
    d(F, X, DF).



% TRIGONOMÉTRICAS

d(sin(F), X, cos(F)*(DF)) :-
    d(F, X, DF).

d(cos(F), X, -sin(F)*(DF)) :-
    d(F, X, DF).

d(tan(F), X, (DF)/(cos(F)^2)) :-
    d(F, X, DF).

d(asin(F), X, (DF)/raiz(1-F^2,2)) :-
    d(F, X, DF).

d(acos(F), X, -(DF)/raiz(1-F^2,2)) :-
    d(F, X, DF).

d(atan(F), X, (DF)/(1+F^2)) :-
    d(F, X, DF).


% TRIGONOMÉTRICAS (HIPERBÓLICAS)

d(sinh(F), X, cosh(F)*(DF)) :-
    d(F, X, DF).

d(cosh(F), X, sinh(F)*(DF)) :-
    d(F, X, DF).

d(tanh(F), X, (DF)/(cosh(F)^2)) :-
    d(F, X, DF).

d(asinh(F), X, (DF)/raiz(F^2+1,2)) :-
    d(F, X, DF).

d(acosh(F), X, (DF)/(raiz(F-1,2)*raiz(F+1,2))) :-
    d(F, X, DF).

d(atanh(F), X, (DF)/(1-F^2)) :-
    d(F, X, DF).













