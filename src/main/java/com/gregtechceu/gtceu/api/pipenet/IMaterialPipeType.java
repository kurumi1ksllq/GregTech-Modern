package com.gregtechceu.gtceu.api.pipenet;

import com.gregtechceu.gtceu.api.data.tag.TagPrefix;

public interface IMaterialPipeType extends IPipeType {

    /**
     * Determines tag prefix used for this pipe type, which gives pipe tag key
     * when combined with pipe's material
     *
     * @return tag prefix used for this pipe type
     */
    TagPrefix getTagPrefix();
}
