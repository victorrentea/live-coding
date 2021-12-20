package lombok.replacerac;

public class ReplaceRAC_TwoArg_ExtraCode {
    private final int dummy;
    private final int dummy2;

    public ReplaceRAC_TwoArg_ExtraCode(int dummy, int dummy2) {
        this.dummy = dummy;
        this.dummy2 = dummy2;
        System.out.println("Extra code");
    }
}
