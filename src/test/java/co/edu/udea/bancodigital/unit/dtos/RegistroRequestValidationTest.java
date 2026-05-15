package co.edu.udea.bancodigital.unit.dtos;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import co.edu.udea.bancodigital.dtos.requests.ActualizarDatosRequest;
import co.edu.udea.bancodigital.dtos.requests.RegistroRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class RegistroRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("CP-REG-02: registro sin email debe ser invalido")
    void registroSinEmail_deberiaSerInvalido() {
        RegistroRequest request = registroRequestValido("Abc123#@");
        ReflectionTestUtils.setField(request, "correo", "");

        assertTieneViolacionEnCampo(validator.validate(request), "correo");
        System.out.println("=== CP-REG-02 RESULTADO OBTENIDO ===");
        System.out.println("Error: " + validator.validate(request).iterator().next().getMessage());
    }

    @Test
    @DisplayName("CP-REG-05: email con formato incorrecto debe ser invalido")
    void registroConEmailInvalido_deberiaSerInvalido() {
        RegistroRequest request = registroRequestValido("Abc123#@");
        ReflectionTestUtils.setField(request, "correo", "juan.gmail.com");

        assertTieneViolacionEnCampo(validator.validate(request), "correo");
        System.out.println("=== CP-REG-05 RESULTADO OBTENIDO ===");
        System.out.println("Error: " + validator.validate(request).iterator().next().getMessage());
    }

    @ParameterizedTest(name = "CP-REG-{0}: contrasena de {1} caracteres debe ser {2}")
    @CsvSource({
        "07,7,invalida,Abc1#23",
        "08,8,valida,Abc123#@",
        "09,9,valida,Abc1234#@",
        "10,15,valida,Abc1234567890#@",
        "11,16,valida,Abc12345678901#@",
        "12,17,invalida,Abc123456789012#@"
    })
    void contrasenaLongitud_deberiaSerValidaOInvalida(String caseId, int length, String expectedOutcome, String contrasena) {
        RegistroRequest request = registroRequestValido(contrasena);

        if ("invalida".equals(expectedOutcome)) {
            assertTieneViolacionEnCampo(validator.validate(request), "contrasena");
        } else {
            assertSinViolacionEnCampo(validator.validate(request), "contrasena");
        }
        System.out.println("=== CP-REG-" + caseId + " RESULTADO OBTENIDO ===");
        if ("invalida".equals(expectedOutcome)) {
            System.out.println("Error: " + validator.validate(request).iterator().next().getMessage());
        } else {
            System.out.println("Registro exitoso con contraseña de longitud " + length + " para usuario " + request.getCorreo());
        }
    }

    @Test
    @DisplayName("CP-REG-13: contrasena sin mayuscula debe ser invalida")
    void contrasenaSinMayuscula_deberiaSerInvalida() {
        RegistroRequest request = registroRequestValido("abc123#@");

        assertTieneViolacionEnCampo(validator.validate(request), "contrasena");
        System.out.println("=== CP-REG-13 RESULTADO OBTENIDO ===");
        System.out.println("Error: " + validator.validate(request).iterator().next().getMessage());
    }

    @Test
    @DisplayName("CP-REG-14: contrasena sin simbolo debe ser invalida")
    void contrasenaSinSimbolo_deberiaSerInvalida() {
        RegistroRequest request = registroRequestValido("Abc12345");

        assertTieneViolacionEnCampo(validator.validate(request), "contrasena");
        System.out.println("=== CP-REG-14 RESULTADO OBTENIDO ===");
        System.out.println("Error: " + validator.validate(request).iterator().next().getMessage());
    }

    @Test
    @DisplayName("CP-UPD-02: email invalido en edicion debe ser invalido")
    void actualizarDatosConEmailInvalido_deberiaSerInvalido() {
        ActualizarDatosRequest request = actualizarDatosRequestValido();
        ReflectionTestUtils.setField(request, "correo", "test.com");

        assertTieneViolacionEnCampo(validator.validate(request), "correo");
        System.out.println("=== CP-UPD-02 RESULTADO OBTENIDO ===");
        System.out.println("Error: " + validator.validate(request).iterator().next().getMessage());
    }

    @Test
    @DisplayName("CP-UPD-03: nombre vacio en edicion debe ser invalido")
    void actualizarDatosConNombreVacio_deberiaSerInvalido() {
        ActualizarDatosRequest request = actualizarDatosRequestValido();
        ReflectionTestUtils.setField(request, "nombre", "");

        assertTieneViolacionEnCampo(validator.validate(request), "nombre");
        System.out.println("=== CP-UPD-03 RESULTADO OBTENIDO ===");
        System.out.println("Error: " + validator.validate(request).iterator().next().getMessage());
    }

    private static RegistroRequest registroRequestValido(String contrasena) {
        RegistroRequest request = new RegistroRequest();
        ReflectionTestUtils.setField(request, "idTipoDoc", 1);
        ReflectionTestUtils.setField(request, "numeroDocumento", "1234567890");
        ReflectionTestUtils.setField(request, "nombre", "Juan");
        ReflectionTestUtils.setField(request, "primerApellido", "Perez");
        ReflectionTestUtils.setField(request, "segundoApellido", "Gomez");
        ReflectionTestUtils.setField(request, "direccion", "Calle 123");
        ReflectionTestUtils.setField(request, "telefono", "3001234567");
        ReflectionTestUtils.setField(request, "correo", "juan@example.com");
        ReflectionTestUtils.setField(request, "contrasena", contrasena);
        return request;
    }

    private static ActualizarDatosRequest actualizarDatosRequestValido() {
        ActualizarDatosRequest request = new ActualizarDatosRequest();
        ReflectionTestUtils.setField(request, "nombre", "Juan");
        ReflectionTestUtils.setField(request, "primerApellido", "Perez");
        ReflectionTestUtils.setField(request, "segundoApellido", "Gomez");
        ReflectionTestUtils.setField(request, "direccion", "Calle 123");
        ReflectionTestUtils.setField(request, "telefono", "3001234567");
        ReflectionTestUtils.setField(request, "correo", "juan@example.com");
        return request;
    }

    private static void assertTieneViolacionEnCampo(Set<? extends ConstraintViolation<?>> violations, String field) {
        assertTrue(tieneViolacionEnCampo(violations, field));
    }

    private static void assertSinViolacionEnCampo(Set<? extends ConstraintViolation<?>> violations, String field) {
        assertFalse(tieneViolacionEnCampo(violations, field));
    }

    private static boolean tieneViolacionEnCampo(Set<? extends ConstraintViolation<?>> violations, String field) {
        return violations.stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals(field));
    }
}
