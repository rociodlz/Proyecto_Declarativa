%Míriam Rocío Doñas López 00022423
%Erick Bladimir Gómez Hernández 00300723
%Henry Salvador Mauricio Mendoza 00183223

:- consult('bc_derivadas.pl').
:- consult('bc_simplificaciones_algebraicas.pl').


integral(F, X, R) :-
    atom(X),
    once(i(F, X, RI)),
    simplificar_algebra(RI, R).


i(C, X, C*X) :-
    number(C).

i(X, X, X^2/2).

i(C*F, X, C*(R)) :-
    number(C),
    i(F, X, R).

% Potencias de x. Admitir exponentes numéricos y fracciones como 1/2.
i(X^E, X, X^M/M) :-
    exponente_numerico(E, N),
    N =\= -1,
    M is N + 1,
    M =\= 0.

i(X^E, X, ln(abs(X))) :-
    exponente_numerico(E, N),
    N =:= -1, !.

% Negación unaria: -F es distinto de (-1)*F
i(-F, X, -R) :-
    i(F, X, R).

% Verifica si el exponente es un valor numérico y no una variable
exponente_numerico(E, N) :-
    ground(E),
    catch(N is E, _, fail),
    number(N).

% Un argumento compuesto solo se admite por sustitución lineal
% si su derivada es una constante no nula.
derivada_constante(F, X, D) :-
    nonvar(F),
    d(F, X, DF),
    simplificar_algebra(DF, SD),
    ground(SD),
    catch(D is SD, _, fail),
    number(D),
    D =\= 0.

% Potencia compuesta con derivada constante (también exponentes fraccionarios).
i(F^E, X, F^M/(D*M)) :-
    F \== X,
    exponente_numerico(E, N),
    N =\= -1,
    derivada_constante(F, X, D),
    M is N + 1,
    M =\= 0.

i(F^E, X, ln(abs(F))/D) :-
    F \== X,
    exponente_numerico(E, N),
    N =:= -1,
    derivada_constante(F, X, D).

% Forma explícita de recíproco, no solo potencias negativas.
i(1/F, X, ln(abs(F))/D) :-
    derivada_constante(F, X, D).

% Integral de una expresión dividida entre una constante
i(F/C, X, R/C) :-
    number(C),
    C =\= 0,
    i(F, X, R).

i(F/C, X, R/C) :-
    number(C),
    C =\= 0,
    i(F, X, R).


i(u(X), X, pendiente(u(X), X)).

i(v(X), X, pendiente(v(X), X)).



i(F+G, X, (RF)+(RG)) :-
    i(F, X, RF),
    i(G, X, RG).

i(F-G, X, (RF)-(RG)) :-
    i(F, X, RF),
    i(G, X, RG).

% Exponencial de base numérica con exponente simple.
i(C^X, X, C^X/ln(C)) :-
    number(C), C > 0, C =\= 1.

% Exponencial de base numérica con exponente lineal.
i(C^F, X, C^F/(D*ln(C))) :-
    number(C), C > 0, C =\= 1,
    F \== X,
    derivada_constante(F, X, D).


% 2 formas de expresar la exponencial por si acaso
i(e^F, X, e^F/D) :-
    derivada_constante(F, X, D).
i(exp(F), X, exp(F)/D) :-
    derivada_constante(F, X, D).



% Integración elemental de logaritmos de argumento lineal.
i(ln(F), X, (F*ln(F)-F)/D) :-
    derivada_constante(F, X, D).

i(log(C,F), X, (F*ln(F)-F)/(D*ln(C))) :-
    number(C), C > 0, C =\= 1,
    derivada_constante(F, X, D).

i(C^u(X), X, pendiente(C^u(X), X)) :-
    number(C),
    C > 0,
    C =\= 1.




% TRIGONOMETRICAS. Caso simple (ya existente) + compuesto lineal.
i(sin(X), X, -cos(X)) :- !.
i(sin(F), X, -cos(F)/D) :-
    derivada_constante(F, X, D).

i(cos(X), X, sin(X)) :- !.
i(cos(F), X, sin(F)/D) :-
    derivada_constante(F, X, D).

i(tan(X), X, -ln(abs(cos(X)))) :- !.
i(tan(F), X, -ln(abs(cos(F)))/D) :-
    derivada_constante(F, X, D).

i(asin(X), X, X*asin(X)+raiz(1-X^2,2)) :- !.
i(asin(F), X, (F*asin(F)+raiz(1-F^2,2))/D) :-
    derivada_constante(F, X, D).

i(acos(X), X, X*acos(X)-raiz(1-X^2,2)) :- !.
i(acos(F), X, (F*acos(F)-raiz(1-F^2,2))/D) :-
    derivada_constante(F, X, D).

i(atan(X), X, X*atan(X)-ln(1+X^2)/2) :- !.
i(atan(F), X, (F*atan(F)-ln(1+F^2)/2)/D) :-
    derivada_constante(F, X, D).

% HIPERBOLICAS. Caso simple (ya existente) + compuesto lineal.
i(sinh(X), X, cosh(X)) :- !.
i(sinh(F), X, cosh(F)/D) :-
    derivada_constante(F, X, D).

i(cosh(X), X, sinh(X)) :- !.
i(cosh(F), X, sinh(F)/D) :-
    derivada_constante(F, X, D).

i(tanh(X), X, ln(cosh(X))) :- !.
i(tanh(F), X, ln(cosh(F))/D) :-
    derivada_constante(F, X, D).

i(asinh(X), X, X*asinh(X)-raiz(X^2+1,2)) :- !.
i(asinh(F), X, (F*asinh(F)-raiz(F^2+1,2))/D) :-
    derivada_constante(F, X, D).

i(acosh(X), X, X*acosh(X)-raiz(X-1,2)*raiz(X+1,2)) :- !.
i(acosh(F), X, (F*acosh(F)-raiz(F-1,2)*raiz(F+1,2))/D) :-
    derivada_constante(F, X, D).

i(atanh(X), X, X*atanh(X)+ln(1-X^2)/2) :- !.
i(atanh(F), X, (F*atanh(F)+ln(1-F^2)/2)/D) :-
    derivada_constante(F, X, D).
