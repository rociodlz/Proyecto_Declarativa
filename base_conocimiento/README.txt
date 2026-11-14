bc_derivadas.pl
bc_integrales.pl
bc_simplificaciones_algebraicas.pl          Simplificación de operaciones básicas
bc_simplificaciones.pl                      Simplificaciones algebraicas adicionales

--------------------------------------------------------------------------------------------------

SIMBOLOGIA

C      = constante numérica
F, G   = funciones o expresiones matemáticas
X      = variable de derivación o integración
N      = índice de una raíz
R      = resultado
DF, DG = derivadas de F y G respectivamente

--------------------------------------------------------------------------------------------------

PRINCIPAL: integral(F,X,R) integral de F respecto a X (aplicando simplificación de expresiones)

d(F,X,R)                    Derivada de F respecto a X
i(F,X,R)                    Integral de F respecto a X (no simplificado)
simplificar(F,R)            Simplificacion de operaciones aritmeticas básicas
simplificar_algebra(F,R)    Simplificaciones algebraicas

--------------------------------------------------------------------------------------------------

OPERADORES BASICOS

F+G
  Argumento 1 (F): primer sumando
  Argumento 2 (G): segundo sumando
  Representación interna: +(F,G)
  Significado: suma

F-G
  Argumento 1 (F): minuendo
  Argumento 2 (G): sustraendo
  Representación interna: -(F,G)
  Significado: resta

-F
  Argumento 1 (F): expresión que se niega
  Representación interna: -(F)
  Significado: negación

F*G
  Argumento 1 (F): primer factor
  Argumento 2 (G): segundo factor
  Representación interna: *(F,G)
  Significado: multiplicación

F/G
  Argumento 1 (F): numerador
  Argumento 2 (G): denominador
  Representación interna: /(F,G)
  Significado: división

F^G
  Argumento 1 (F): base
  Argumento 2 (G): exponente
  Representación interna: ^(F,G)
  Significado: potencia

--------------------------------------------------------------------------------------------------

FUNCIONES MATEMÁTICAS

Declaración       Argumentos en orden                         Significado
raiz(F,N)         F: radicando; N: índice                     Raíz N-ésima de F
ln(F)             F: argumento                                Logaritmo natural
log(C,F)          C: base; F: argumento                       Logaritmo de F en base C
abs(F)            F: argumento                                Valor absoluto
sin(F)            F: argumento                                Seno
cos(F)            F: argumento                                Coseno
tan(F)            F: argumento                                Tangente
asin(F)           F: argumento                                Arcoseno
acos(F)           F: argumento                                Arcocoseno
atan(F)           F: argumento                                Arcotangente
sinh(F)           F: argumento                                Seno hiperbólico
cosh(F)           F: argumento                                Coseno hiperbólico
tanh(F)           F: argumento                                Tangente hiperbólica
asinh(F)          F: argumento                                Seno hiperbólico inverso
acosh(F)          F: argumento                                Coseno hiperbólico inverso
atanh(F)          F: argumento                                Tangente hiperbólica inversa

**Las funciones trigonométricas se interpretan en radianes.

e^F   = exponencial natural de F
C^F   = exponencial de base C y exponente F


pendiente(F,X)
  Argumento 1 (F): función que sigue sin integrar
  Argumento 2 (X): variable de integración
  Significado: integral no resuelta

u(X)
  Argumento 1 (X): argumento de la función
  Significado: función simbólica u

v(X)
  Argumento 1 (X): argumento de la función
  Significado: función simbólica v

du(X)
  Argumento 1 (X): argumento de la función
  Significado: derivada simbólica de u

dv(X)
  Argumento 1 (X): argumento de la función
  Significado: derivada simbólica de v