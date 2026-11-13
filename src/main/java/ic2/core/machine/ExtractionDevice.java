package ic2.core.machine;

import net.minecraft.world.MenuProvider;

interface ExtractionDevice extends MenuProvider {
    void tick();
    void dropContents();
}
