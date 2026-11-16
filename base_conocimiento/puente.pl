/*  puente.pl - Punto de entrada que usa Java (docs/contrato.md, secciones 5 a 7).

    Uso:
        swipl base_conocimiento/puente.pl "<expresion_normalizada>" <a> <b>

    Escribe en la salida estándar UNA sola línea:
        OK|<primitiva>|<integral_definida>|<area_geometrica>
        ERROR|<CODIGO>|<detalle>

    <area_geometrica> es "-" cuando no se calculó (area_geometrica/5 no existe o falló).
    Las advertencias de SWI-Prolog van a la salida de error y Java las ignora.

    Carga, desde la misma carpeta que este archivo:
        bc_integrales.pl   integral/3
        bc_evaluacion.pl   integrar/2, evaluar_area/4 y, opcionalmente, area_geometrica/5

    Códigos que genera este archivo:
        SINTAXIS      term_string/2 no puede leer la expresión.
        NO_SOPORTADA  integrar/2 falla.
        DOMINIO       evaluar_area/4 lanza un error de evaluación o de dominio.
        INTERNO       argumentos incorrectos, archivos faltantes o cualquier otra excepción.
*/

:- initialization(main, main).

:- dynamic directorio_puente/1.
:- prolog_load_context(directory, Dir), assertz(directorio_puente(Dir)).

archivo_base('bc_integrales.pl').
archivo_base('bc_evaluacion.pl').


main :-
    set_stream(user_output, encoding(utf8)),
    current_prolog_flag(argv, Argumentos),
    catch(responder(Argumentos, Linea), Error, linea_interno(Error, Linea)),
    format("~w~n", [Linea]),
    flush_output.


%   responder(+Argumentos, -Linea)  Nunca falla: siempre produce una línea OK| o ERROR|.

responder([TextoF, TextoA, TextoB], Linea) :- !,
    cargar_base,
    leer_limite(a, TextoA, A),
    leer_limite(b, TextoB, B),
    (   leer_expresion(TextoF, F)
    ->  resolver(F, A, B, Linea)
    ;   format(string(Detalle), "No se pudo leer la expresión: ~w", [TextoF]),
        linea_error('SINTAXIS', Detalle, Linea)
    ).
responder(Argumentos, Linea) :-
    length(Argumentos, N),
    format(string(Detalle),
           "Se esperaban 3 argumentos (expresión, a, b) y se recibieron ~d", [N]),
    linea_error('INTERNO', Detalle, Linea).


resolver(F, A, B, Linea) :-
    (   integrar(F, P)
    ->  catch(evaluar(F, P, A, B, Linea), Error, error_evaluacion(Error, Linea))
    ;   format(string(Detalle), "No hay una regla de integración para ~w", [F]),
        linea_error('NO_SOPORTADA', Detalle, Linea)
    ).

evaluar(F, P, A, B, Linea) :-
    evaluar_area(P, A, B, V0),
    V is float(V0),
    area_geometrica_texto(F, P, A, B, TextoArea),
    format(string(Linea), "OK|~w|~w|~w", [P, V, TextoArea]).

%   El área geométrica es opcional: si no existe o falla, se informa "-" (nunca un valor inventado).
area_geometrica_texto(F, P, A, B, Texto) :-
    (   current_predicate(area_geometrica/5),
        catch(area_geometrica(F, P, A, B, Ag0), _, fail),
        number(Ag0)
    ->  Ag is float(Ag0),
        format(string(Texto), "~w", [Ag])
    ;   Texto = "-"
    ).


%   Lectura de los argumentos

leer_expresion(Texto, F) :-
    Texto \== '',
    catch(term_string(F, Texto), error(syntax_error(_), _), fail),
    ground(F),
    F \== end_of_file.

%   argv entrega átomos y number_string/2 solo acepta cadenas: se convierte antes.
leer_limite(_, Texto, N) :-
    text_to_string(Texto, Cadena),
    catch(number_string(N, Cadena), _, fail), !.
leer_limite(Nombre, Texto, _) :-
    throw(puente(limite_invalido(Nombre, Texto))).


cargar_base :-
    directorio_puente(Dir),
    forall(archivo_base(Archivo), cargar_archivo(Dir, Archivo)).

cargar_archivo(Dir, Archivo) :-
    directory_file_path(Dir, Archivo, Ruta),
    (   exists_file(Ruta)
    ->  load_files(Ruta, [if(not_loaded), silent(true)])
    ;   throw(puente(falta_archivo(Archivo)))
    ).


%   Errores

error_evaluacion(Error, Linea) :-
    (   motivo_dominio(Error, Motivo)
    ->  format(string(Detalle), "Error de dominio al evaluar la primitiva: ~w", [Motivo]),
        linea_error('DOMINIO', Detalle, Linea)
    ;   linea_interno(Error, Linea)
    ).

motivo_dominio(error(evaluation_error(Tipo), _), Tipo).
motivo_dominio(error(domain_error(Tipo, Valor), _), Tipo-Valor).
motivo_dominio(error(dominio(Motivo), _), Motivo).
motivo_dominio(dominio(Motivo), Motivo).

linea_interno(puente(falta_archivo(Archivo)), Linea) :- !,
    format(string(Detalle), "No se encontró ~w junto a puente.pl", [Archivo]),
    linea_error('INTERNO', Detalle, Linea).
linea_interno(puente(limite_invalido(Nombre, Texto)), Linea) :- !,
    format(string(Detalle), "El límite ~w no es un número: ~w", [Nombre, Texto]),
    linea_error('INTERNO', Detalle, Linea).
linea_interno(Error, Linea) :-
    format(string(Detalle), "Excepción inesperada en Prolog: ~q", [Error]),
    linea_error('INTERNO', Detalle, Linea).

%   El detalle no puede contener "|" ni saltos de línea: rompería el formato de la respuesta.
linea_error(Codigo, Detalle, Linea) :-
    split_string(Detalle, "|\n\r", "", Partes),
    atomic_list_concat(Partes, ' ', Limpio0),
    sub_atom_trunc(Limpio0, 300, Limpio),
    format(string(Linea), "ERROR|~w|~w", [Codigo, Limpio]).

sub_atom_trunc(Atomo, Max, Atomo) :-
    atom_length(Atomo, L), L =< Max, !.
sub_atom_trunc(Atomo, Max, Corto) :-
    sub_atom(Atomo, 0, Max, _, Inicio),
    atom_concat(Inicio, '...', Corto).
