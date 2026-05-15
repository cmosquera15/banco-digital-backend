Feature: Consulta de cuenta bancaria del cliente

  Scenario: Consulta exitosa
    Given que el cliente esta autenticado en el sistema
    And tiene cuenta registrada
    When solicita consultar su cuenta
    Then puede ver el numero de cuenta saldo y estado

  Scenario: Usuario sin cuenta bancaria asignada
    Given que el cliente esta autenticado en el sistema
    And no tiene cuenta registrada
    When solicita consultar su cuenta sin registros
    Then puede ver un mensaje Cuenta no disponible
