Feature: Trade recommendation scoring

  Scenario: A concentrated holding is reduced before deploying excess cash
    Given a portfolio worth "36395.00" with positions
      | symbol | name                   | assetClass | marketValue | allocationPercent |
      | VOO    | Vanguard S&P 500 ETF   | ETF        | 8689.50     | 23.88             |
      | AAPL   | Apple Inc.             | Equity     | 4111.80     | 11.30             |
      | MSFT   | Microsoft Corp.        | Equity     | 4381.50     | 12.04             |
      | BTC    | Bitcoin                | Crypto     | 11862.20    | 32.59             |
      | CASH   | Available cash         | Cash       | 7350.00     | 20.19             |
    When recommendations are generated
    Then the first recommendation should be to "Reduce" "VOO"
    And the estimated amount should be "3231.88"
    And the confidence should be "High"
