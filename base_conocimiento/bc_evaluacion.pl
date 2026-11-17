%Míriam Rocío Doñas López 00022423
%Erick Bladimir Gómez Hernández 00300723
%Henry Salvador Mauricio Mendoza 00183223


/*  bc_evaluacion.pl - Adaptador entre la base de conocimiento y puente.pl
    (docs/contrato.md, secciones 3 y 9).

    PROPUESTA: el contrato deja este archivo con responsable "por acordar".
    No modifica los predicados de Henry: solo usa integral/3.

    integrar(+F, -P)
        P es la primitiva simplificada de F respecto de x. Falla si no hay regla
        o si P contiene pendiente(_, _). Si falta un predicado de la base de
        conocimiento (archivo incompleto) lanza el error en vez de fallar, para
        que puente.pl lo informe como INTERNO y no como NO_SOPORTADA.

    evaluar_area(+P, +A, +B, -V)
        V = P(B) - P(A) (regla de Barrow). Lanza error(dominio(Motivo), _) si P no
        es continua en [A, B] (sección 9). Los errores de evaluación de SWI-Prolog
        (ln(0), acosh(0.5), ...) se propagan tal cual; puente.pl los convierte en
        DOMINIO.

    area_geometrica(+F, +P, +A, +B, -Ag)   (opcional)
        Ag = ∫|F| dx en [min(A,B), max(A,B)], siempre >= 0: divide el intervalo
        en los puntos donde F puede cambiar de signo y suma |P(x[i+1]) - P(x[i])|.
        FALLA (puente.pl informa "-") si esos puntos no se pueden obtener de
        forma confiable: nunca devuelve un área dudosa.

    Ceros y singularidades
        Los puntos donde una expresión se anula se calculan de forma ANALÍTICA
        cuando la expresión es reconocible (ceros/4):
            polinomios en x (raíces reales exactas por aislamiento con la derivada),
            sin, cos, tan de un argumento lineal (todas las raíces kπ, π/2 + kπ),
            productos, cocientes, potencias, constantes por expresión, y funciones
            con un único cero (sinh, tanh, asin, atan, asinh, atanh, ln, ...).
        Solo si la expresión no es reconocible (por ejemplo sin(x) + x) se recurre
        al muestreo, y su resultado se descarta si no es estable al duplicar la
        cantidad de muestras.

    Notación de la primitiva (base_conocimiento/README.txt): ln(F) es el logaritmo
    natural, log(C, F) el logaritmo en base C y raiz(F, N) la raíz N-ésima. Antes
    de evaluar con is/2 se traducen a la aritmética de SWI-Prolog.
*/

:- ensure_loaded('bc_integrales.pl').


% ---------------------------------------------------------------------------
% integrar/2

integrar(F, P) :-
    catch(integral(F, x, P0), Error, error_de_integracion(Error)),
    \+ contiene_pendiente(P0),
    simplificar_primitiva(P0, P).

%   Un predicado inexistente indica una base de conocimiento incompleta: se
%   propaga. Cualquier otro error significa que no hay una regla aplicable.
error_de_integracion(Error) :-
    Error = error(existence_error(procedure, _), _), !,
    throw(Error).
error_de_integracion(_) :-
    fail.

contiene_pendiente(T) :-
    sub_term(S, T),
    nonvar(S),
    S = pendiente(_, _), !.


% ---------------------------------------------------------------------------
% simplificar_primitiva/2
%
% integral/3 ya simplifica con simplificar_algebra/2 (bc_simplificaciones_algebraicas.pl),
% pero ese simplificador no tiene reglas para constantes que multiplican fracciones
% o sumas, exponentes negativos ni signos. Por eso quedaban primitivas como
% 3*(x^3/3), x^ -1/ -1 o 2*(x^2/2+x). Esta pasada se aplica solo a la primitiva
% que se devuelve a Java; no cambia el simplificador de Henry, que también usa
% bc_derivadas.pl.
%
% Las reglas recorren la expresión de abajo hacia arriba y se repiten hasta que
% el resultado no cambia (punto fijo), con un máximo de pasadas para que nunca
% pueda haber un ciclo. Todas son identidades válidas en todo el dominio de la
% expresión original: no se cancelan factores con x (x/x), no se combinan
% potencias de potencias ni raíces, de modo que la primitiva simplificada está
% definida exactamente donde lo estaba la original.

maximo_pasadas_simplificacion(50).

simplificar_primitiva(P0, P) :-
    maximo_pasadas_simplificacion(Maximo),
    simplificar_hasta_punto_fijo(Maximo, P0, P).

simplificar_hasta_punto_fijo(0, P, P) :- !.
simplificar_hasta_punto_fijo(N, P0, P) :-
    pasada_simplificacion(P0, P1),
    (   P1 == P0
    ->  P = P0
    ;   N1 is N - 1,
        simplificar_hasta_punto_fijo(N1, P1, P)
    ).

pasada_simplificacion(T, R) :-
    compound(T), !,
    T =.. [Operador|Argumentos],
    maplist(pasada_simplificacion, Argumentos, Simplificados),
    T1 =.. [Operador|Simplificados],
    (   regla_primitiva(T1, R0)
    ->  R = R0
    ;   R = T1
    ).
pasada_simplificacion(T, T).

%   regla_primitiva(+Expresion, -Simplificada)  C, C1, C2 y D son números.

% Neutros y signos
regla_primitiva(F*C, F) :- number(C), C =:= 1, !.
regla_primitiva(C*F, F) :- number(C), C =:= 1, !.
regla_primitiva(C*F, -F) :- number(C), C =:= -1, !.
regla_primitiva(F/D, F) :- number(D), D =:= 1, !.
regla_primitiva(-(-F), F) :- !.
regla_primitiva(-C, N) :- number(C), !, N is -C.
regla_primitiva(-(C*F), N*F) :- number(C), !, N is -C.
regla_primitiva(-(N/F), M/F) :- number(N), !, M is -N.
regla_primitiva(-(F/D), (-F)/D) :- number(D), !.
regla_primitiva(C*(-F), N*F) :- number(C), !, N is -C.
regla_primitiva(F/D, -(F/E)) :- number(D), D < 0, !, E is -D.

% Constantes que multiplican productos, fracciones y sumas
regla_primitiva(C1*(C2*F), N*F) :- number(C1), number(C2), producto_exacto(C1, C2, N), !.
regla_primitiva(C*(N/F), M/F) :- number(C), number(N), producto_exacto(C, N, M), !.
regla_primitiva(C*(F/D), R) :- number(C), number(D), D =\= 0, !, coeficiente_sobre(C, D, F, R).
regla_primitiva((C*F)/D, R) :- number(C), number(D), D =\= 0, !, coeficiente_sobre(C, D, F, R).
regla_primitiva((N/F)/D, R) :- number(N), number(D), D =\= 0, !, numerador_sobre(N, D, F, R).
regla_primitiva(C*(F+G), C*F+C*G) :- number(C), !.
regla_primitiva(C*(F-G), C*F-C*G) :- number(C), !.

% Sumas y restas de términos negativos
regla_primitiva(F+(-G), F-G) :- !.
regla_primitiva(F+C*G, F-N*G) :- number(C), C < 0, !, N is -C.
regla_primitiva(F-(-G), F+G) :- !.
regla_primitiva(F-C*G, F+N*G) :- number(C), C < 0, !, N is -C.
regla_primitiva(F+C, F-N) :- number(C), C < 0, !, N is -C.

% Exponentes enteros negativos: F^(-N) = 1/F^N (mismo dominio: F distinto de 0)
regla_primitiva(F^N, 1/F) :- integer(N), N =:= -1, !.
regla_primitiva(F^N, 1/F^M) :- integer(N), N < -1, !, M is -N.

%   Los coeficientes solo se operan cuando el resultado es exacto: con enteros, o
%   cuando uno de los factores es 1 o -1. Con decimales (0.5, 1.1) se conserva la
%   forma de fracción, para no introducir aproximaciones de coma flotante como
%   0.16666666666666666 en lugar de 0.5/3.

%   producto_exacto(+A, +B, -P)  P = A*B, solo si el producto es exacto.
producto_exacto(A, B, P) :-
    (   integer(A), integer(B)
    ;   abs(A) =:= 1
    ;   abs(B) =:= 1
    ), !,
    P is A * B.

%   coeficiente_sobre(+C, +D, +F, -R)  R = C*F/D con el coeficiente reducido.
coeficiente_sobre(C, D, F, R) :-
    integer(C), integer(D), !,
    G is gcd(C, D),
    C1 is C // G, D1 is D // G,
    (   D1 < 0 -> C2 is -C1, D2 is -D1 ; C2 = C1, D2 = D1 ),
    con_coeficiente(C2, F, T),
    (   D2 =:= 1 -> R = T ; R = T/D2 ).
coeficiente_sobre(C, D, F, (C*F)/D).      % decimales: se conserva la fracción

%   numerador_sobre(+N, +D, +F, -R)  R = (N/F)/D = N/(D*F) con N/D reducido.
numerador_sobre(N, D, F, R) :-
    integer(N), integer(D), !,
    G is gcd(N, D),
    N1 is N // G, D1 is D // G,
    (   D1 < 0 -> N2 is -N1, D2 is -D1 ; N2 = N1, D2 = D1 ),
    (   D2 =:= 1 -> R = N2/F ; R = N2/(D2*F) ).
numerador_sobre(N, D, F, N/(D*F)).        % decimales: se conserva la fracción

con_coeficiente(K, F, F) :- K =:= 1, !.
con_coeficiente(K, F, -F) :- K =:= -1, !.
con_coeficiente(K, F, K*F).


% ---------------------------------------------------------------------------
% evaluar_area/4

%   La continuidad se verifica ANTES de evaluar los extremos: así, si un extremo es
%   una singularidad (x^(-1) en [0, 1], atanh(x) en [-1, 0]), el error dice que la
%   función no está definida en ese punto, y no que el valor es demasiado grande.
%   Un desbordamiento auténtico (2^x en [0, 1000000]) sigue llegando como
%   float_overflow al evaluar.
evaluar_area(P, A, B, V) :-
    verificar_continuidad(P, A, B),
    valor(P, B, VB),
    valor(P, A, VA),
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
aritmetica(T, _, T).                           % constantes como e o pi

aritmetica_en(X, T, R) :-
    aritmetica(T, X, R).

sin_x(E) :-
    \+ ( sub_term(S, E), S == x ).

%   constante(+E, -C)  E no depende de x y se puede evaluar.
constante(E, C) :-
    sin_x(E),
    catch(valor(E, 0, C), _, fail).


% ---------------------------------------------------------------------------
% Singularidades (sección 9)
%
% La regla de Barrow solo vale si P es continua en [A, B]. Se buscan en P las
% subexpresiones que dejan de estar definidas donde otra expresión G se anula:
%     B^N con N < 0          G = B
%     F / D, D no numérico   G = D
%     ln(H), log(C, H)       G = H
%     tan(H)                 G = cos(H)
% Si G tiene un cero (o un polo) en [A, B], se lanza DOMINIO. Los ceros de G se
% calculan con ceros/4 (analítico); el muestreo solo se usa si G no es
% reconocible. Además se evalúa P en los extremos, lo que detecta argumentos
% fuera del dominio (asin(2), acosh(0.5), raíz de un negativo, ...).

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
    sin_abs(G0, G),
    \+ sin_x(G).

expresion_que_no_se_anula(Base ^ N, Base) :- number(N), N < 0.
expresion_que_no_se_anula(_ / D, D) :- \+ number(D).
expresion_que_no_se_anula(ln(H), H).
expresion_que_no_se_anula(log(_, H), H).
expresion_que_no_se_anula(tan(H), cos(H)).

%   abs(H) se anula donde H se anula, pero nunca cambia de signo: se revisa H.
sin_abs(abs(H), G) :- !, sin_abs(H, G).
sin_abs(G, G).

%   se_anula(+G, +Inferior, +Superior)  G vale 0 (o no está definida) en algún
%   punto de [Inferior, Superior].
se_anula(G, Inferior, Superior) :-
    catch(ceros(G, Inferior, Superior, Ceros), demasiados_ceros, Ceros = muchos),
    !,
    (   Ceros == muchos
    ->  true
    ;   Ceros \== []
    ->  true
    ;   no_evaluable_en(G, [Inferior, Superior])
    ).
se_anula(G, Inferior, Superior) :-        % G no reconocible: muestreo
    (   muestras_necesarias(Inferior, Superior, N)
    ->  catch(muestrear(G, Inferior, Superior, N, Muestras),
              error(evaluation_error(_), _),
              Muestras = no_evaluable),
        (   Muestras == no_evaluable
        ->  true
        ;   pairs_values(Muestras, Valores),
            cambia_de_signo(Valores)
        )
    ;   %   Intervalo demasiado largo para muestrearlo con una separación
        %   pequeña: no se puede garantizar la continuidad, no se adivina.
        format(atom(Motivo),
               "no se puede verificar que ~w no se anule en [~w, ~w]",
               [G, Inferior, Superior]),
        throw(error(dominio(Motivo), _))
    ).

no_evaluable_en(G, Puntos) :-
    member(X, Puntos),
    \+ catch(valor(G, X, _), error(evaluation_error(_), _), fail), !.

cambia_de_signo([V|_]) :- V =:= 0, !.
cambia_de_signo([V1, V2|_]) :- sign(V1) * sign(V2) < 0, !.
cambia_de_signo([_|Vs]) :- cambia_de_signo(Vs).


% ---------------------------------------------------------------------------
% ceros/4: puntos de [Inferior, Superior] donde E se anula o tiene un polo
%
% ceros(+E, +Inferior, +Superior, -Puntos)
%   Puntos (ordenados, sin repetir) contiene TODOS los x de [Inferior, Superior]
%   donde E vale 0 o no está definida por una división entre cero; fuera de
%   ellos E no cambia de signo. Falla si E no es reconocible. Lanza
%   demasiados_ceros si hay más de maximo_ceros/1 puntos.

maximo_ceros(200000).

ceros(E, Inferior, Superior, Puntos) :-
    ceros_(E, Inferior, Superior, Puntos0),
    msort(Puntos0, Ordenados),
    sin_repetir(Ordenados, Puntos).

ceros_(E, _, _, []) :-
    sin_x(E), !.
ceros_(E, Inferior, Superior, Puntos) :-
    polinomio(E, Coeficientes), !,
    raices_polinomio(Coeficientes, Inferior, Superior, Puntos).
ceros_(-E, I, S, P) :- !, ceros_(E, I, S, P).
ceros_(abs(E), I, S, P) :- !, ceros_(E, I, S, P).
ceros_(E1 * E2, I, S, P) :- !,
    ceros_(E1, I, S, P1), ceros_(E2, I, S, P2), append(P1, P2, P), limitar(P).
ceros_(E1 / E2, I, S, P) :- !,
    ceros_(E1, I, S, P1), ceros_(E2, I, S, P2), append(P1, P2, P), limitar(P).
ceros_(C ^ E, _, _, []) :-                 % exponencial C^E con C > 0
    constante(C, VC), VC > 0, !,
    sin_x(C), \+ sin_x(E).
ceros_(E ^ N, I, S, P) :-
    sin_x(N), !,
    ceros_(E, I, S, P).
ceros_(raiz(E, _), I, S, P) :- !, ceros_(E, I, S, P).
ceros_(sqrt(E), I, S, P) :- !, ceros_(E, I, S, P).
ceros_(exp(_), _, _, []) :- !.
ceros_(cosh(_), _, _, []) :- !.
ceros_(sin(L), I, S, P) :- !, ceros_trigonometricos(L, 0.0, I, S, P).
ceros_(cos(L), I, S, P) :- !, ceros_trigonometricos(L, pi / 2, I, S, P).
ceros_(tan(L), I, S, P) :- !,             % ceros en kπ y polos en π/2 + kπ
    ceros_trigonometricos(L, 0.0, I, S, P1),
    ceros_trigonometricos(L, pi / 2, I, S, P2),
    append(P1, P2, P), limitar(P).
ceros_(Funcion, I, S, P) :-               % un único cero, donde el argumento vale Raiz
    Funcion =.. [Nombre, L],
    cero_unico(Nombre, Raiz), !,
    (   Raiz =:= 0 -> ceros_(L, I, S, P) ; ceros_(L - Raiz, I, S, P) ).
ceros_(ln(L), I, S, P) :- !, ceros_(L - 1, I, S, P).
ceros_(log(_, L), I, S, P) :- !, ceros_(L - 1, I, S, P).

cero_unico(sinh, 0).
cero_unico(tanh, 0).
cero_unico(asinh, 0).
cero_unico(atan, 0).
cero_unico(asin, 0).
cero_unico(atanh, 0).
cero_unico(acos, 1).
cero_unico(acosh, 1).

limitar(Puntos) :-
    length(Puntos, N),
    maximo_ceros(Maximo),
    (   N > Maximo -> throw(demasiados_ceros) ; true ).

sin_repetir([], []).
sin_repetir([X], [X]) :- !.
sin_repetir([X, Y|Resto], Salida) :-
    (   abs(X - Y) =< 1.0e-12 * max(1.0, abs(X))
    ->  sin_repetir([X|Resto], Salida)
    ;   Salida = [X|Salida1],
        sin_repetir([Y|Resto], Salida1)
    ).

%   ceros_trigonometricos(+L, +Desfase, +I, +S, -Puntos)  x de [I, S] con
%   L(x) = Desfase + kπ, para L lineal (m·x + q). Falla si L no es lineal.
ceros_trigonometricos(L, Desfase0, I, S, Puntos) :-
    polinomio(L, Coeficientes),
    lineal(Coeficientes, Q, M),
    Desfase is Desfase0,
    (   M =:= 0
    ->  (   Q0 is Q - Desfase, abs(sin(Q0)) < 1.0e-12 -> Puntos = [I, S] ; Puntos = [] )
    ;   L1 is M * I + Q, L2 is M * S + Q,
        Lmin is min(L1, L2), Lmax is max(L1, L2),
        K1 is ceiling((Lmin - Desfase) / pi),
        K2 is floor((Lmax - Desfase) / pi),
        Cantidad is K2 - K1 + 1,
        maximo_ceros(Maximo),
        (   Cantidad > Maximo -> throw(demasiados_ceros) ; true ),
        findall(X,
                (   between(K1, K2, K),
                    X0 is (Desfase + K * pi - Q) / M,
                    X is max(I, min(S, X0))
                ),
                Puntos)
    ).

lineal([Q], Q, 0) :- !.
lineal([Q, M], Q, M).


% ---------------------------------------------------------------------------
% Polinomios: coeficientes en orden creciente de grado ([c0, c1, c2, ...]).

maximo_grado(40).

polinomio(x, [0, 1]) :- !.
polinomio(E, [C]) :- constante(E, C), !.
polinomio(A + B, P) :- !,
    polinomio(A, PA), polinomio(B, PB), sumar_pol(PA, PB, P).
polinomio(A - B, P) :- !,
    polinomio(A, PA), polinomio(B, PB),
    escalar_pol(-1, PB, NB), sumar_pol(PA, NB, P).
polinomio(-A, P) :- !,
    polinomio(A, PA), escalar_pol(-1, PA, P).
polinomio(A * B, P) :- !,
    polinomio(A, PA), polinomio(B, PB), multiplicar_pol(PA, PB, P),
    grado_permitido(P).
polinomio(A / B, P) :- !,
    constante(B, C), C =\= 0,
    polinomio(A, PA), Inversa is 1 / C, escalar_pol(Inversa, PA, P).
polinomio(A ^ N, P) :-
    constante(N, VN),
    integer_valor(VN, K), K >= 0,
    maximo_grado(Maximo),
    polinomio(A, PA),
    length(PA, LA),
    (LA - 1) * K =< Maximo,
    potencia_pol(PA, K, P).

integer_valor(V, K) :-
    (   integer(V) -> K = V
    ;   float(V), V =:= round(V), K is round(V)
    ).

grado_permitido(P) :-
    length(P, L), maximo_grado(Maximo), L - 1 =< Maximo.

sumar_pol([], B, B) :- !.
sumar_pol(A, [], A) :- !.
sumar_pol([A|As], [B|Bs], [C|Cs]) :- C is A + B, sumar_pol(As, Bs, Cs).

escalar_pol(K, P, Q) :- maplist([A, B]>>(B is K * A), P, Q).

multiplicar_pol([], _, []).
multiplicar_pol([A|As], B, P) :-
    escalar_pol(A, B, AB),
    multiplicar_pol(As, B, Resto),
    (   Resto == [] -> P = AB ; sumar_pol(AB, [0|Resto], P) ).

potencia_pol(_, 0, [1]) :- !.
potencia_pol(P, K, Q) :-
    K1 is K - 1, potencia_pol(P, K1, Q1), multiplicar_pol(P, Q1, Q).

derivada_pol([_|Cs], D) :- derivada_pol_(Cs, 1, D).
derivada_pol_([], _, []).
derivada_pol_([C|Cs], N, [D|Ds]) :- D is N * C, N1 is N + 1, derivada_pol_(Cs, N1, Ds).

evaluar_pol(P, X, V) :-
    reverse(P, Invertido),
    foldl([C, A0, A]>>(A is A0 * X + C), Invertido, 0, V).

%   Quita los coeficientes nulos de mayor grado.
normalizar_pol(P, Q) :-
    reverse(P, R), quitar_ceros(R, R1), reverse(R1, Q).
quitar_ceros([C|Cs], R) :- C =:= 0, !, quitar_ceros(Cs, R).
quitar_ceros(Cs, Cs).

%   raices_polinomio(+P, +I, +S, -Raices)  Raíces reales de P en [I, S]. Entre
%   dos raíces consecutivas de P' el polinomio es monótono, así que tiene a lo
%   sumo una raíz, que se encuentra por bisección. Un polinomio idénticamente
%   nulo se trata como "cero en todo el intervalo" (se devuelven I y S).
raices_polinomio(P0, I, S, Raices) :-
    normalizar_pol(P0, P),
    (   P == []
    ->  Raices = [I, S]
    ;   raices_(P, I, S, Raices)
    ).

raices_([_], _, _, []) :- !.                         % constante no nula
raices_([C0, C1], I, S, Raices) :- !,
    X is -C0 / C1,
    (   X >= I, X =< S -> Raices = [X] ; Raices = [] ).
raices_(P, I, S, Raices) :-
    derivada_pol(P, D0), normalizar_pol(D0, D),
    raices_(D, I, S, Criticos),
    append([I|Criticos], [S], Extremos),
    escala_pol(P, I, S, Escala),
    tramos(P, Extremos, Escala, Raices0),
    msort(Raices0, Raices1),
    sin_repetir(Raices1, Raices).

escala_pol(P, I, S, Escala) :-
    M is max(1.0, max(abs(I), abs(S))),
    reverse(P, Invertido),
    foldl([C, A0, A]>>(A is A0 * M + abs(C)), Invertido, 0, Escala0),
    Escala is Escala0 * 1.0e-12.

tramos(P, [X1, X2|Xs], Escala, Raices) :- !,
    evaluar_pol(P, X1, V1),
    evaluar_pol(P, X2, V2),
    (   abs(V1) =< Escala -> R1 = [X1] ; R1 = [] ),
    (   abs(V1) > Escala, abs(V2) > Escala, sign(V1) * sign(V2) < 0
    ->  biseccion(evaluar_pol(P), X1, V1, X2, 200, Cero), R2 = [Cero]
    ;   R2 = []
    ),
    tramos(P, [X2|Xs], Escala, Resto),
    append([R1, R2, Resto], Raices).
tramos(P, [X], Escala, Raices) :- !,
    evaluar_pol(P, X, V),
    (   abs(V) =< Escala -> Raices = [X] ; Raices = [] ).
tramos(_, [], _, []).

%   biseccion(+F, +A, +VA, +B, +Iteraciones, -Cero)  F es un cierre F(X, V).
biseccion(F, A, VA, B, Iteraciones, Cero) :-
    Medio is (A + B) / 2,
    (   (Iteraciones =:= 0 ; Medio =< min(A, B) ; Medio >= max(A, B))
    ->  Cero = Medio
    ;   call(F, Medio, VM),
        Restantes is Iteraciones - 1,
        (   VM =:= 0
        ->  Cero = Medio
        ;   sign(VA) * sign(VM) < 0
        ->  biseccion(F, A, VA, Medio, Restantes, Cero)
        ;   biseccion(F, Medio, VM, B, Restantes, Cero)
        )
    ).


% ---------------------------------------------------------------------------
% Muestreo (solo para expresiones que ceros/4 no reconoce)

%   El muestreo solo se usa con una separación máxima de 0.05 entre puntos
%   (las funciones de la base oscilan, como mucho, con período 2π) y con a lo
%   sumo maximo_muestras/1 puntos; si el intervalo es más largo no se muestrea.
separacion_maxima(0.05).
minimo_muestras(4000).
maximo_muestras(40000).

muestras_necesarias(Inferior, Superior, N) :-
    separacion_maxima(H),
    minimo_muestras(Minimo),
    maximo_muestras(Maximo),
    N is max(Minimo, ceiling((Superior - Inferior) / H)),
    N =< Maximo.

%   muestrear(+Expresion, +Inferior, +Superior, +N, -Muestras)  Pares X-V en N+1
%   puntos equidistantes (uno solo si el intervalo es un punto).
muestrear(Expresion, Inferior, Superior, N0, Muestras) :-
    (   Inferior =:= Superior -> N = 0 ; N = N0 ),
    findall(X-V,
            (   between(0, N, I),
                X is Inferior + (Superior - Inferior) * I / max(N, 1),
                valor(Expresion, X, V)
            ),
            Muestras).

%   cambios_por_muestreo(+F, +I, +S, -Ceros)  Cambios de signo de F. Se muestrea
%   dos veces, con N y con ~1.618·N puntos (cantidades no proporcionales, para
%   que un posible solapamiento no se repita igual). Falla si las cuentas no
%   coinciden, si el intervalo es demasiado largo o si hay tantos cambios que el
%   muestreo no es confiable.
cambios_por_muestreo(F, I, S, Ceros) :-
    muestras_necesarias(I, S, N),
    N2 is round(N * 1.618) + 1,
    catch(( muestrear(F, I, S, N, M1), muestrear(F, I, S, N2, M2) ), _, fail),
    ceros_en(F, M1, C1),
    ceros_en(F, M2, Ceros),
    length(C1, K), length(Ceros, K),
    K =< N // 20.

ceros_en(F, [X1-V1, X2-V2|Resto], Ceros) :- !,
    (   V2 =:= 0, Resto \== []
    ->  Ceros = [X2|Ceros1]
    ;   sign(V1) * sign(V2) < 0
    ->  biseccion(valor(F), X1, V1, X2, 200, Cero),
        Ceros = [Cero|Ceros1]
    ;   Ceros = Ceros1
    ),
    ceros_en(F, [X2-V2|Resto], Ceros1).
ceros_en(_, _, []).


% ---------------------------------------------------------------------------
% area_geometrica/5 (opcional)

area_geometrica(F, P, A, B, Area) :-
    Inferior is min(A, B),
    Superior is max(A, B),
    (   Inferior =:= Superior
    ->  Area = 0.0
    ;   puntos_de_corte(F, Inferior, Superior, Cortes),
        include([X]>>(X > Inferior, X < Superior), Cortes, Interiores),
        append([Inferior|Interiores], [Superior], Puntos),
        sumar_tramos(P, Puntos, 0.0, Area)
    ).

%   Puntos interiores donde F puede cambiar de signo. Si F no es reconocible se
%   usa el muestreo verificado; si tampoco es confiable, falla (área "-").
puntos_de_corte(F, I, S, Cortes) :-
    catch(ceros(F, I, S, Cortes0), demasiados_ceros, fail), !,
    Cortes = Cortes0.
puntos_de_corte(F, I, S, Cortes) :-
    \+ catch(ceros(F, I, S, _), demasiados_ceros, true),
    cambios_por_muestreo(F, I, S, Cortes).

sumar_tramos(P, [X1, X2|Xs], Acumulado, Area) :- !,
    valor(P, X1, V1),
    valor(P, X2, V2),
    Acumulado1 is Acumulado + abs(V2 - V1),
    sumar_tramos(P, [X2|Xs], Acumulado1, Area).
sumar_tramos(_, _, Area, Area).
