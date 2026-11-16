/*  bc_evaluacion.pl - Adaptador entre la base de conocimiento y puente.pl
    (docs/contrato.md, secciones 3 y 9).

    PROPUESTA: el contrato deja este archivo con responsable "por acordar".
    No modifica los predicados de Henry: solo usa integral/3.

    integrar(+F, -P)
        P es la primitiva simplificada de F respecto de x. Falla si no hay regla
        o si P contiene pendiente(_, _). Nunca lanza excepciones.

    evaluar_area(+P, +A, +B, -V)
        V = P(B) - P(A) (regla de Barrow). Lanza error(dominio(Motivo), _) si P no
        es continua en [A, B] en los casos conocidos (sección 9). Los errores de
        evaluación de SWI-Prolog (ln(0), acosh(0.5), ...) se propagan tal cual;
        puente.pl los convierte en DOMINIO.

    area_geometrica(+F, +P, +A, +B, -Ag)   (opcional)
        Ag = ∫|F| dx en [A, B]: divide el intervalo en los cambios de signo de F
        y suma |P(x[i+1]) - P(x[i])| en cada tramo.

    Notación de la primitiva (base_conocimiento/README.txt): ln(F) es el logaritmo
    natural, log(C, F) el logaritmo en base C y raiz(F, N) la raíz N-ésima. Antes
    de evaluar con is/2 se traducen a la aritmética de SWI-Prolog.
*/

:- ensure_loaded('bc_integrales.pl').


% ---------------------------------------------------------------------------
% integrar/2

integrar(F, P) :-
    catch(integral(F, x, P), _, fail),
    \+ contiene_pendiente(P).

contiene_pendiente(T) :-
    sub_term(S, T),
    nonvar(S),
    S = pendiente(_, _), !.


% ---------------------------------------------------------------------------
% evaluar_area/4

evaluar_area(P, A, B, V) :-
    valor(P, B, VB),
    valor(P, A, VA),
    verificar_continuidad(P, A, B),
    V is VB - VA.

%   valor(+Expresion, +X0, -V)  Evalúa la expresión en x = X0 (como float).
valor(Expresion, X0, V) :-
    X is float(X0),
    aritmetica(Expresion, X, E),
    V is E.

%   aritmetica(+Expresion, +X, -E)  Sustituye x por X y traduce la notación de la
%   base de conocimiento a funciones que is/2 sabe evaluar.
aritmetica(x, X, X) :- !.
aritmetica(N, _, N) :- number(N), !.
aritmetica(ln(F), X, log(G)) :- !,
    aritmetica(F, X, G).
aritmetica(log(C, F), X, log(G) / log(H)) :- !,
    aritmetica(C, X, H),
    aritmetica(F, X, G).
aritmetica(raiz(F, N), X, R) :- !,
    aritmetica(F, X, G),
    aritmetica(N, X, M),
    (   integer(N), N mod 2 =:= 1
    ->  R = sign(G) * abs(G) ** (1 / M)        % raíz impar de un negativo
    ;   R = G ** (1 / M)
    ).
aritmetica(T, X, R) :-
    compound(T), !,
    T =.. [Operador|Argumentos],
    maplist(aritmetica_en(X), Argumentos, Traducidos),
    R =.. [Operador|Traducidos].
aritmetica(T, _, T).                           % constantes como e

aritmetica_en(X, T, R) :-
    aritmetica(T, X, R).


% ---------------------------------------------------------------------------
% Singularidades conocidas (sección 9)
%
% La regla de Barrow solo vale si P es continua en [A, B]. Se buscan en P las
% subexpresiones que dejan de estar definidas donde otra expresión G se anula:
%     B^N con N < 0          G = B
%     F / D, D no numérico   G = D
%     ln(H), log(C, H)       G = H
%     tan(H)                 G = cos(H)
% Si alguna G se anula (o cambia de signo) en [A, B], se lanza DOMINIO.
% Limitación (documentada en el contrato): no garantiza encontrar todas las
% singularidades de cualquier expresión.

verificar_continuidad(P, A, B) :-
    Inferior is min(A, B),
    Superior is max(A, B),
    forall(critica(P, Termino, G),
           (   se_anula(G, Inferior, Superior)
           ->  format(atom(Motivo),
                      "~w no está definida en algún punto de [~w, ~w]",
                      [Termino, Inferior, Superior]),
               throw(error(dominio(Motivo), _))
           ;   true
           )).

critica(P, Termino, G) :-
    sub_term(Termino, P),
    compound(Termino),
    expresion_que_no_se_anula(Termino, G0),
    sin_abs(G0, G).

expresion_que_no_se_anula(Base ^ N, Base) :- number(N), N < 0.
expresion_que_no_se_anula(_ / D, D) :- \+ number(D).
expresion_que_no_se_anula(ln(H), H).
expresion_que_no_se_anula(log(_, H), H).
expresion_que_no_se_anula(tan(H), cos(H)).

%   abs(H) se anula donde H se anula, pero nunca cambia de signo: se revisa H.
sin_abs(abs(H), G) :- !, sin_abs(H, G).
sin_abs(G, G).

%   se_anula(+G, +Inferior, +Superior)  G vale 0, cambia de signo o no se puede
%   evaluar en alguno de los puntos de muestreo del intervalo.
se_anula(G, Inferior, Superior) :-
    catch(muestrear(G, Inferior, Superior, Muestras),
          error(evaluation_error(_), _),
          Muestras = no_evaluable),
    (   Muestras == no_evaluable
    ->  true
    ;   pairs_values(Muestras, Valores),
        cambia_de_signo(Valores)
    ).

cambia_de_signo([V|_]) :- V =:= 0, !.
cambia_de_signo([V1, V2|_]) :- sign(V1) * sign(V2) < 0, !.
cambia_de_signo([_|Vs]) :- cambia_de_signo(Vs).


%   muestrear(+Expresion, +Inferior, +Superior, -Muestras)  Pares X-V en 2001
%   puntos equidistantes (uno solo si el intervalo es un punto).
puntos_de_muestreo(2000).

muestrear(Expresion, Inferior, Superior, Muestras) :-
    (   Inferior =:= Superior
    ->  N = 0
    ;   puntos_de_muestreo(N)
    ),
    findall(X-V,
            (   between(0, N, I),
                X is Inferior + (Superior - Inferior) * I / max(N, 1),
                valor(Expresion, X, V)
            ),
            Muestras).


% ---------------------------------------------------------------------------
% area_geometrica/5 (opcional)

area_geometrica(F, P, A, B, Area) :-
    Inferior is min(A, B),
    Superior is max(A, B),
    ceros(F, Inferior, Superior, Ceros),
    append([Inferior|Ceros], [Superior], Puntos),
    sumar_tramos(P, Puntos, 0.0, Area).

sumar_tramos(P, [X1, X2|Xs], Acumulado, Area) :- !,
    valor(P, X1, V1),
    valor(P, X2, V2),
    Acumulado1 is Acumulado + abs(V2 - V1),
    sumar_tramos(P, [X2|Xs], Acumulado1, Area).
sumar_tramos(_, _, Area, Area).

%   ceros(+F, +Inferior, +Superior, -Ceros)  Puntos interiores donde F cambia de
%   signo, en orden creciente.
ceros(_, Inferior, Superior, []) :-
    Inferior =:= Superior, !.
ceros(F, Inferior, Superior, Ceros) :-
    muestrear(F, Inferior, Superior, Muestras),
    ceros_en(F, Muestras, Ceros).

ceros_en(F, [X1-V1, X2-V2|Resto], Ceros) :- !,
    (   V2 =:= 0, Resto \== []
    ->  Ceros = [X2|Ceros1]
    ;   sign(V1) * sign(V2) < 0
    ->  biseccion(F, X1, V1, X2, 60, Cero),
        Ceros = [Cero|Ceros1]
    ;   Ceros = Ceros1
    ),
    ceros_en(F, [X2-V2|Resto], Ceros1).
ceros_en(_, _, []).

biseccion(_, A, _, B, 0, Cero) :- !,
    Cero is (A + B) / 2.
biseccion(F, A, VA, B, Iteraciones, Cero) :-
    Medio is (A + B) / 2,
    valor(F, Medio, VM),
    Restantes is Iteraciones - 1,
    (   VM =:= 0
    ->  Cero = Medio
    ;   sign(VA) * sign(VM) < 0
    ->  biseccion(F, A, VA, Medio, Restantes, Cero)
    ;   biseccion(F, Medio, VM, B, Restantes, Cero)
    ).
