package com.loliball.appliedcreate

import appeng.api.integrations.igtooltip.BaseClassRegistration
import appeng.api.integrations.igtooltip.TooltipProvider
import com.loliball.appliedcreate.block.AndesitePatternProviderBlock
import com.loliball.appliedcreate.block.BrassPatternProviderBlock
import com.loliball.appliedcreate.block.entity.AndesitePatternProviderBlockEntity
import com.loliball.appliedcreate.block.entity.BrassPatternProviderBlockEntity

class AppliedCreateTooltipProvider : TooltipProvider {
    override fun registerBlockEntityBaseClasses(registration: BaseClassRegistration) {
        registration.addBaseBlockEntity(
            AndesitePatternProviderBlockEntity::class.java,
            AndesitePatternProviderBlock::class.java
        )
        registration.addBaseBlockEntity(
            BrassPatternProviderBlockEntity::class.java,
            BrassPatternProviderBlock::class.java
        )
    }
}
