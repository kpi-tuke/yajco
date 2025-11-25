package yajco.grammar.semlang;

/**
 * Action that filters elements from a source list by type and adds them to a target list.
 *
 * Generates code like:
 * for (Object element : sourceList.getWrappedObject()) {
 *     if (element instanceof TargetType) {
 *         targetList.add((TargetType) element);
 *     }
 * }
 */
public class FilterByTypeAction extends Action {
    private final RValue sourceList;
    private final String targetClassName;
    private final LValue targetList;

    public FilterByTypeAction(RValue sourceList, String targetClassName, LValue targetList) {
        this.sourceList = sourceList;
        this.targetClassName = targetClassName;
        this.targetList = targetList;
    }

    public RValue getSourceList() {
        return sourceList;
    }

    public String getTargetClassName() {
        return targetClassName;
    }

    public LValue getTargetList() {
        return targetList;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.FILTER_BY_TYPE;
    }
}
