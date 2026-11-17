%Míriam Rocío Doñas López 00022423
%Erick Bladimir Gómez Hernández 00300723
%Henry Salvador Mauricio Mendoza 00183223

% Variables y valores atomicos
simplificar(F, F) :-
    var(F), !.

simplificar(F, F) :-
    atomic(F), !.


% SUMAS

simplificar(F+G, R) :-
    !,
    simplificar(F, SF),
    simplificar(G, SG),
    simplificar_suma(SF, SG, R).

simplificar_suma(0, G, G) :- !.
simplificar_suma(F, 0, F) :- !.

simplificar_suma(F, G, R) :-
    number(F),
    number(G), !,
    R is F+G.

simplificar_suma(F, G, F+G).



% RESTAS Y NEGACION

simplificar(F-G, R) :-
    !,
    simplificar(F, SF),
    simplificar(G, SG),
    simplificar_resta(SF, SG, R).

simplificar_resta(F, 0, F) :- !.

% Una expresion menos si misma
simplificar_resta(F, F, 0) :- !.

% Restar valores numericos
simplificar_resta(F, G, R) :-
    number(F),
    number(G), !,
    R is F-G.

% Cero menos una expresion
simplificar_resta(0, G, R) :-
    !,
    simplificar_negacion(G, R).

simplificar_resta(F, G, F-G).


% Negacion
simplificar(-F, R) :-
    !,
    simplificar(F, SF),
    simplificar_negacion(SF, R).

simplificar_negacion(0, 0) :- !.

simplificar_negacion(-F, F) :- !.

simplificar_negacion(F, R) :-
    number(F), !,
    R is -F.

simplificar_negacion(F, -F).




% 3. MULTIPLICACIONES

simplificar(F*G, R) :-
    !,
    simplificar(F, SF),
    simplificar(G, SG),
    simplificar_producto(SF, SG, R).

simplificar_producto(0, _, 0) :- !.
simplificar_producto(_, 0, 0) :- !.

simplificar_producto(1, G, G) :- !.
simplificar_producto(F, 1, F) :- !.

simplificar_producto(F, G, R) :-
    number(F),
    number(G), !,
    R is F*G.

simplificar_producto(F, C, C*(F)) :-
    number(C), !.

simplificar_producto(F, G, (F)*(G)).



% 4. DIVISIONES

simplificar(F/G, R) :-
    !,
    simplificar(F, SF),
    simplificar(G, SG),
    simplificar_division(SF, SG, R).

simplificar_division(F, 1, F) :- !.

simplificar_division(0, G, 0) :-
    number(G),
    G =\= 0, !.

simplificar_division(F, G, R) :-
    integer(F),
    integer(G),
    G =\= 0,
    0 is F mod G, !,
    R is F // G.

simplificar_division(F, G, F/G).




% 5. POTENCIAS

simplificar(F^G, R) :-
    !,
    simplificar(F, SF),
    simplificar(G, SG),
    simplificar_potencia(SF, SG, R).

% Exponente uno
simplificar_potencia(F, 1, F) :- !.

% Exponente cero, excluyendo base cero
simplificar_potencia(F, 0, 1) :-
    F \== 0, !.

% Base uno
simplificar_potencia(1, _, 1) :- !.

simplificar_potencia(F, G, F^G).




% 6. FUNCIONES COMPUESTAS

% Simplificar argumentos de funciones:
% sin, cos, ln, raiz, etc.


simplificar(F, R) :-
    compound(F),
    F =.. [Nombre|Argumentos],
    maplist(simplificar, Argumentos, Nuevos),
    R =.. [Nombre|Nuevos].


% 1. compound() reconoce que F es una expresión compuesta de otras (ej: sin(2*x + 8))
% 2. [Nombre|Argumentos] separa cada expresion que compone la original (ej: sin() y 2*x + 8 )
% 3. maplist(simplificar, Argumentos, Nuevos) aplica simplificar a cada elemento de Argumentos y Nuevos toma los resultados (aun separados)
% 4. [Nombre|Nuevos] une los elementos en el orden antes de que fueran separados en [Nombre|Argumentos]
