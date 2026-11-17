%Míriam Rocío Doñas López 00022423
%Erick Bladimir Gómez Hernández 00300723
%Henry Salvador Mauricio Mendoza 00183223

:- consult('bc_simplificaciones.pl').




%simplifica individualmente cada expresion de F, verifica si el conjunto F puede simplificarse mas
%de poder simplificarse mas vuelve a ejecutarse
%de no poder simplificarse mas lo devuelve como R

simplificar_algebra(F, R) :-
    simplificar(F, SF),
    paso_algebra(SF, SR),
    ( SF == SR ->
        R = SF
    ;
        simplificar_algebra(SR, R)
    ).



% RECORRER EXPRESIONES

paso_algebra(F, R) :-
    compound(F), !,
    F =.. [Nombre|Argumentos],
    maplist(simplificar_algebra,
            Argumentos, Nuevos),
    SF =.. [Nombre|Nuevos],
    transformar_algebra(SF, R).

paso_algebra(F, F).




% MULTIPLICACIONES ALGEBRAICAS

transformar_algebra(C1*(C2*F), R) :-
    number(C1),
    number(C2), !,
    C is C1*C2,
    R = C*F.

transformar_algebra((C*F)*(G), C*(F*G)) :-
    number(C), !.

transformar_algebra((F)*(C*G), C*(F*G)) :-
    number(C), !.

transformar_algebra((F)*(F), (F)^2) :- !.




% SUMAS ALGEBRAICAS

transformar_algebra((C1*F)+(C2*F), R) :-
    number(C1),
    number(C2),
    !,
    C is C1+C2,
    R = C*F.

transformar_algebra(F+F, 2*F) :- !.

transformar_algebra(F+(C*F), R) :-
    number(C),
    !,
    N is C+1,
    R = N*F.

transformar_algebra((C*F)+F, R) :-
    number(C),
    !,
    N is C+1,
    R = N*F.




% RESTAS ALGEBRAICAS


transformar_algebra((C1*F)-(C2*G), R) :-
    number(C1),
    number(C2),
    F == G, !,
    C is C1-C2,
    R = C*F.


transformar_algebra((C*F)-F, R) :-
    number(C),
    !,
    N is C-1,
    R = N*F.




% DIVISIONES ALGEBRAICAS

transformar_algebra((C*F)/D, R) :-
    integer(C),
    integer(D),
    D =\= 0,
    0 is C mod D, !,
    N is C // D,
    R = N*(F).




% CASO GENERAL

% Conservar expresiones que no
% tienen una simplificacion aplicable
transformar_algebra(F, F).
