# Rendu

## Informations

- Nom : à renseigner
- Prénom : à renseigner
- Campus : à renseigner

## Défauts trouvés en partie A

| Règle | Défaut constaté dans le code fourni | Test qui le démontre |
|---|---|---|
| C3 | Un crédit de zéro était accepté alors que le montant doit être strictement positif. | `shouldRejectNonPositiveCredit` |
| C5 | Un débit atteignant exactement la limite du découvert autorisé était refusé. | `shouldAllowDebitExactlyAtAuthorizedOverdraftLimit` |
| C6 | `estVide()` comparait les `BigDecimal` avec `equals`, qui distingue les échelles (`0` et `0.00`). | `shouldConsiderEveryRepresentationOfZeroAsEmpty` |
| F3 | Les frais étaient offerts aux titulaires de 26 ans, alors que la règle concerne les moins de 26 ans. | `shouldApplyBalanceThreshold` (cas âge 26, solde 999,99 €) |
| F4 | Le seuil était strictement supérieur à 1 000 € au lieu d’être supérieur ou égal. | `shouldApplyBalanceThreshold` (cas 1 000,00 €) |
| F5 | Le retour anticipé lorsque les frais de tenue étaient offerts supprimait aussi le supplément de carte premium. | `shouldAlwaysChargePremiumSupplementWhenMaintenanceIsFree` |

Les autres règles de `Compte` et `CalculFrais` ne présentaient pas de défaut constaté dans le code fourni.

## Couverture de code

Ajouter ici la capture IntelliJ obtenue avec **Run with Coverage** après exécution des tests.

## Partie B

Aucune règle laissée inachevée dans le code actuel : V1 à V7 sont implémentées et testées.
