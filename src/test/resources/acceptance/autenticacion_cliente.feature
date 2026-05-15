Feature: Autenticacion de cliente

  Scenario: Login exitoso
    Given que el cliente esta registrado en el sistema
    And tiene credenciales validas
    When ingresa su email y contrasena correctos
    Then el sistema autentica al cliente
    And permite acceso a los servicios

  Scenario: Primer intento de login fallido
    Given que el cliente tiene 0 intento fallido previo
    When ingresa credenciales incorrectas
    Then puede ver un mensaje "Credenciales invalidas"

  Scenario: Segundo intento de login fallido
    Given que el cliente tiene 1 intento fallido previo
    When ingresa credenciales incorrectas
    Then puede ver un mensaje "Credenciales invalidas"

  Scenario: Tercer intento consecutivo fallido - Bloqueo
    Given que el cliente tiene 2 intentos fallidos consecutivos previos
    When ingresa credenciales incorrectas por tercera vez
    Then el sistema bloquea el acceso del cliente

  Scenario: Login despues de bloqueo temporal
    Given que el cliente tuvo 3 intentos fallidos consecutivos
    And han pasado 5 minutos desde el bloqueo
    When ingresa su email y contrasena correctos
    Then puede acceder nuevamente a su cuenta

  Scenario: Usuario no registrado
    Given que el email ingresado no esta registrado
    When intenta iniciar sesion
    Then puede ver un mensaje Usuario no encontrado
