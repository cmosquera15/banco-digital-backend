Feature: Actualizar datos cliente

  Scenario: Actualizacion exitosa
    Given que el cliente esta autenticado en el sistema
    When modifica su nombre o correo con valores validos y guarda los cambios
    Then puede ver un mensaje confirmando la actualizacion
    And puede ver sus datos actualizados
