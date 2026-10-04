package family.cookbook.importer;

import family.cookbook.importer.IngredientLineParser.Line;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class IngredientLineParserTest {

    @ParameterizedTest(name = "[{0}] {1}")
    @CsvSource(delimiter = '|', nullValues = "-", value = {
            // language | line                                | amount | unit           | name
            "hu | 1 kg Burgonya                                 | 1      | kg             | burgonya",
            "hu | 40 dkg Kolbász félszáraz, füstölt             | 40     | dkg            | kolbász félszáraz, füstölt",
            "hu | 0 ízlés szerint Só                            | -      | ízlés szerint  | só",
            "hu | Só                                            | -      | -              | só",
            "hu | Só ízlés szerint                              | -      | ízlés szerint  | só",
            "hu | 2 púpozott kanál liszt                        | 2      | púpozott kanál | liszt",
            "hu | 1,5 dl tej                                    | 1.5    | dl             | tej",
            "hu | 3-4 ek tejföl                                 | -      | 3–4 ek         | tejföl",
            "hu | 2 gerezd fokhagyma                            | 2      | gerezd         | fokhagyma",
            "hu | 1 csomag sütőpor                              | 1      | csomag         | sütőpor",
            "hu | 6 db Főtt tojás                               | 6      | db             | főtt tojás",
            "hu | 250g liszt                                    | 250    | g              | liszt",
            "de | 2 EL Zucker                                   | 2      | EL             | Zucker",
            "de | 1 Prise Salz                                  | 1      | Prise          | Salz",
            "de | 500 g Kartoffeln, festkochend                 | 500    | g              | Kartoffeln, festkochend",
            "de | Salz und Pfeffer nach Belieben                | -      | nach Belieben  | Salz und Pfeffer",
            "de | 1 Pck. Vanillezucker                          | 1      | Pck.           | Vanillezucker",
            "en | 1 ½ cups of flour                             | 1.5    | cups           | flour",
            "en | 1/2 tsp salt                                  | 0.5    | tsp            | salt",
            "en | 2 large Eggs                                  | 2      | -              | large Eggs",
            "en | Lemon zest                                    | -      | -              | lemon zest",
    })
    void splitsAmountUnitAndName(String language, String text, String amount, String unit, String name) {
        Line line = IngredientLineParser.parse(text, language);

        assertThat(line.amount()).isEqualTo(amount == null ? null : new BigDecimal(amount));
        assertThat(line.unit()).isEqualTo(unit);
        assertThat(line.name()).isEqualTo(name);
    }
}
