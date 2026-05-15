package co.edu.udea.bancodigital.dtos.requests;

import co.edu.udea.bancodigital.dtos.validation.ValidDocumentoPorTipo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@ValidDocumentoPorTipo
public class RegistroRequest {

    @NotNull(message = "El id del tipo de documento es obligatorio")
    @Min(value = 1, message = "El id del tipo de documento debe ser mayor a cero")
    private Integer idTipoDoc;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 50, message = "El número de documento no puede superar 50 caracteres")
    private String numeroDocumento;

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜáéíóúüÑñ ]+$", message = "El nombre solo puede contener letras y espacios")
    private String nombre;

    @NotBlank(message = "El primer apellido es obligatorio")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜáéíóúüÑñ ]+$", message = "El primer apellido solo puede contener letras y espacios")
    private String primerApellido;

    @Pattern(regexp = "^$|^[A-Za-zÁÉÍÓÚÜáéíóúüÑñ ]+$", message = "El segundo apellido solo puede contener letras y espacios")
    @Size(max = 100, message = "El segundo apellido no puede superar 100 caracteres")
    private String segundoApellido;

    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^3\\d{9}$", message = "El teléfono debe ser celular colombiano: 10 dígitos iniciando en 3")
    private String telefono;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato válido")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 16, message = "La contraseña debe tener entre 8 y 16 caracteres")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$", message = "La contraseña debe incluir al menos 1 mayúscula, 1 número y 1 símbolo")
    private String contrasena;
}