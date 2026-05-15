Feature: Administracion de clientes

  Scenario: Consulta exitosa
    Given que el administrador esta autenticado en el sistema
    And existen clientes registrados
    When solicita la lista de clientes
    Then puede ver la lista de clientes con su informacion basica

  Scenario: No hay clientes registrados
    Given que el administrador esta autenticado en el sistema
    And no existen clientes registrados
    When solicita la lista de clientes
    Then puede ver un mensaje indicando que no hay clientes registrados
