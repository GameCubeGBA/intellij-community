// "Extract common part with variables from if " "true"

import java.util.List;
import java.util.Map;

public class Main {

  private void work(int i){};

  public void test(int a, int b) {
    if(true) {
      int c = a + b;
      work(1);
      return c;<caret>
    } else {
      int c = a - b;
      work(1);
      return c;
    }
  }
}