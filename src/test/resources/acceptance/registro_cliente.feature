Feature: Registro de cliente

  Scenario: Registro Exitoso
    Given que el usuario no esta registrado en el sistema
    And ingresa sus datos personales completos y validos
    When envia la solicitud de registro
    Then puede ver un mensaje de registro exitoso

  Scenario: Registro datos incompleto
    Given que el usuario no esta registrado en el sistema
    And omite el correo obligatorio
    When envia la solicitud de registro
    Then puede ver un mensaje indicando que faltan datos obligatorios

  Scenario: Registro con documento duplicado
    Given que ya existe un cliente registrado con el mismo numero de documento
    When envia la solicitud de registro
    Then puede ver un mensaje indicando que el documento ya esta registrado

  Scenario: Registro con email duplicado
    Given que ya existe un cliente registrado con el mismo correo electronico
    When envia la solicitud de registro
    Then puede ver un mensaje indicando que el correo ya esta registrado

  Scenario: Registro con email de formato invalido
    Given que el usuario no esta registrado en el sistema
    And el email no tiene formato valido
    When envia la solicitud de registro
    Then puede ver un mensaje indicando que el correo no tiene un formato valido

  Scenario Outline: Registro con contrasena invalida
    Given que el usuario no esta registrado en el sistema
    And ingresa la contrasena "<contrasena>"
    When envia la solicitud de registro
    Then puede ver un mensaje indicando que la contrasena es invalida

    Examples:
      | contrasena             |
      | pass123                |
      | password1122334455#!!  |
      | password123            |
