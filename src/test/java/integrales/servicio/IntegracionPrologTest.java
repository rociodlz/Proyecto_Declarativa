        ResultadoIntegral r = resolver("2^x", 0, 1_000_000);
        assertEquals(ResultadoIntegral.DOMINIO, r.codigoError(), r.toString());
        assertTrue(r.detalle().contains("demasiado grande"), r.detalle());
        assertFalse(r.detalle().contains("no está definida"), r.detalle());
    }

    // Casos que pasaron a estar soportados tras ampliar las reglas de integración.
    // Se compara el valor definido, no la forma textual de la primitiva.
    @ParameterizedTest(name = "{0}: ∫ {1} de 1 a 2 = {2}")
    @CsvSource(delimiter = ';', value = {
            "32;  e^x;      4.670774270472",
            "34;  -x;      -1.500000000000",
            "35;  x/2;      0.750000000000",
            "36;  sin(2*x);  0.118748392158",
            "S10; exp(x);   4.670774270472",
            "S11; ln(x);    0.386294361120",
            "S12; log(x);   0.386294361120",
    })
    void integralAhoraSoportada(String caso, String f, double esperado) {
        ResultadoIntegral r = resolver(f, 1, 2);
        assertTrue(r.exito(), "caso " + caso + ": " + r);
        assertNotNull(r.primitiva(), "caso " + caso + ": falta la primitiva");
        assertEquals(esperado, r.integralDefinida(), TOLERANCIA, "caso " + caso + ": " + r);
    }

    // Expresiones que siguen sin una regla de integración.
    @ParameterizedTest(name = "{0}: ∫ {1} → NO_SOPORTADA")
    @CsvSource(delimiter = ';', value = {
            "33;   x*3",
            "S13;  e",
            "M4;   x*sin(x)",
    })
    void integralNoSoportada(String caso, String f) {
        ResultadoIntegral r = resolver(f, 1, 2);
        assertEquals(ResultadoIntegral.NO_SOPORTADA, r.codigoError(), "caso " + caso + ": " + r);
    }

    // Caso 50: con un tiempo máximo mínimo la consulta real a SWI-Prolog se corta.
    @Test
    void consultaQueTardaDemasiadoEsTiempoAgotado() {
        ServicioProlog impaciente = new ServicioProlog(
                new ConfiguracionProlog(config.swipl().orElseThrow(), config.directorioProlog(), 1));
        ResultadoIntegral r = impaciente.resolver(new SolicitudIntegral("3*x^2", 0, 2));
        assertEquals(ResultadoIntegral.TIEMPO_AGOTADO, r.codigoError(), r.toString());
    }

    // Simplificación de la primitiva: la forma devuelta y el valor de la integral.
    @ParameterizedTest(name = "∫ {0} = {1}")
    @CsvSource(delimiter = ';', value = {
            "3*x^2;         x^3;                0; 2; 8.000000",
            "x^(-2);        -1/x;               1; 2; 0.500000",
            "2*(x+1);       x^2+2*x;            0; 2; 8.000000",
            "x^3-x;         x^4/4-x^2/2;        -2; 2; 0.000000",
            "3*x;           3*x^2/2;            1; 3; 12.000000",
            "x+-1;          x^2/2-x;            0; 1; -0.500000",
            "3*sin(x);      -3*cos(x);          0; 3.141593; 6.000000",
            "0.5*x;         0.5*x^2/2;          0; 2; 1.000000",
            // Coeficientes decimales: se conserva la fracción, sin aproximaciones como 0.1666…
            "0.5*x^2;       0.5*x^3/3;          0; 1; 0.166667",
            "1.1*x^2;       1.1*x^3/3;          0; 1; 0.366667",
            "0.7*x^(-3);    -0.7/(2*x^2);       1; 2; 0.262500",
            "0.3*x^(-2);    -0.3/x;             1; 2; 0.150000",
            "2.5*(x+1);     2.5*x^2/2+2.5*x;    0; 1; 3.750000",
            "x^(-3);        -1/(2*x^2);         1; 2; 0.375000",
            "2*x^(-2);      -2/x;               1; 2; 1.000000",
            "x^2-2*x+1;     x^3/3-x^2+x;        0; 2; 0.666667",
            "x^(-1);        ln(abs(x));         1; 2; 0.693147",
            "asin(x);       x*asin(x)+raiz(1-x^2,2); 0; 1; 0.570796",
    })
    void primitivaSimplificada(String f, String primitiva, double a, double b, double integral) {
        ResultadoIntegral r = resolver(f, a, b);
        assertTrue(r.exito(), f + ": " + r);
        assertEquals(primitiva, r.primitiva(), f);
        assertEquals(integral, r.integralDefinida(), TOLERANCIA, f);
    }

    // Sección 4, caso 37: las funciones simbólicas nunca se resuelven.
    @Test
    void funcionSimbolicaNoSoportada() {
        assertEquals(ResultadoIntegral.NO_SOPORTADA, resolver("u(x)", 0, 1).codigoError());
    }

    @Test
    void expresionIlegibleEsSintaxis() {
        assertEquals(ResultadoIntegral.SINTAXIS, resolver("3*x^", 0, 1).codigoError());
    }

    // Regresión A3: una base de conocimiento incompleta es INTERNO, nunca NO_SOPORTADA.

    /** Copia los archivos de Prolog a una carpeta temporal y devuelve un servicio que la usa. */
    private ServicioProlog copiaDeLaBase() throws IOException {
        for (String archivo : ConfiguracionProlog.ARCHIVOS_PROLOG) {
            Files.copy(config.directorioProlog().resolve(archivo), tmp.resolve(archivo),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        return new ServicioProlog(new ConfiguracionProlog(config.swipl().orElseThrow(), tmp, 10_000));
    }

    @Test   // caso 96
    void faltaUnArchivoDeLaBase() throws IOException {
        ServicioProlog copia = copiaDeLaBase();
        Files.delete(tmp.resolve("bc_derivadas.pl"));
        ResultadoIntegral r = copia.resolver(new SolicitudIntegral("3*x^2", 0, 2));
        assertEquals(ResultadoIntegral.INTERNO, r.codigoError(), r.toString());
        assertTrue(r.detalle().contains("bc_derivadas.pl"), r.detalle());
    }

    @Test   // caso 97
    void faltaUnPredicadoDeLaBase() throws IOException {
        ServicioProlog copia = copiaDeLaBase();
        Files.writeString(tmp.resolve("bc_simplificaciones.pl"), "");
        ResultadoIntegral r = copia.resolver(new SolicitudIntegral("3*x^2", 0, 2));
        assertEquals(ResultadoIntegral.INTERNO, r.codigoError(), r.toString());
        assertTrue(r.detalle().contains("simplificar"), r.detalle());
    }

    @Test   // caso 98
    void errorDeSintaxisEnLaBase() throws IOException {
        ServicioProlog copia = copiaDeLaBase();
        Files.writeString(tmp.resolve("bc_derivadas.pl"), "roto(\n", StandardOpenOption.APPEND);
        ResultadoIntegral r = copia.resolver(new SolicitudIntegral("3*x^2", 0, 2));
        assertEquals(ResultadoIntegral.INTERNO, r.codigoError(), r.toString());
        assertTrue(r.detalle().contains("cargar"), r.detalle());
    }
}
