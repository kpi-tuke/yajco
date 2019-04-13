package yajco.model;

import java.util.List;

/**
 * Specifies notation of compound parameters which can contain multiple notation parts which should be linked.
 */
public interface CompoundNotationPart extends NotationPart {
    List<NotationPart> getParts();
    void addPart(NotationPart part);
}
