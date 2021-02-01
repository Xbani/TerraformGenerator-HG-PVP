package org.terraform.structure.village.plains.forge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.terraform.coregen.PopulatorDataAbstract;
import org.terraform.coregen.TerraLootTable;
import org.terraform.data.Wall;
import org.terraform.structure.room.jigsaw.JigsawType;
import org.terraform.utils.BlockUtils;
import org.terraform.utils.GenUtils;
import org.terraform.utils.blockdata.ChestBuilder;
import org.terraform.utils.blockdata.DirectionalBuilder;

public class PlainsVillageForgeWeaponSmithPiece extends PlainsVillageForgeStandardPiece {

	public PlainsVillageForgeWeaponSmithPiece(int widthX, int height, int widthZ, JigsawType type, BlockFace[] validDirs) {
		super(widthX, height, widthZ, type, validDirs);
	}
	
	//Use postBuildDecoration.
    @Override
    public void postBuildDecoration(Random random, PopulatorDataAbstract data) {
    	//SimpleBlock core = new SimpleBlock(data, this.getRoom().getX(), this.getRoom().getY(), this.getRoom().getZ());
    	if(this.getWalledFaces().size() == 0) {
    		//Leave the room empty if you can't spawn anything.
    		return;
    	}
    	
    	ArrayList<BlockFace> walledFaces = this.getWalledFaces();
    	Collections.shuffle(walledFaces);
    	boolean placedJobBlock = false;
    	for(BlockFace face:walledFaces) {
    		Entry<Wall,Integer> entry = this.getRoom().getWall(data, face, 0);
    		Wall w = entry.getKey();
    		for(int i = 0; i < entry.getValue(); i++) {
    			
    			//Ensure that you don't place anything against the entrance
    			if(w.getRear().getRelative(0,-1,0).getType() != Material.CHISELED_STONE_BRICKS)
    			{
    				int choice = random.nextInt(4);
    				switch(choice) {
    				case 0:
    					if(!placedJobBlock){
    						placedJobBlock = true;
    						w.setType(Material.SMITHING_TABLE, Material.FLETCHING_TABLE);
    					}
    					break;
    				case 1: //Workbench & lantern
    					if(GenUtils.chance(random, 3, 4))
    						break;
    					w.setType(Material.CRAFTING_TABLE);
    					w.getRelative(0,1,0).setType(Material.LANTERN);
    					if(w.getRelative(0,1,0).getRear().getType().isSolid() && !placedJobBlock) {
    						placedJobBlock = true;
    						new DirectionalBuilder(Material.GRINDSTONE)
        					.setFacing(w.getDirection())
        					.apply(w.getRelative(0,1,0));
    					}
    					break;
    				case 2: //Anvil
    					if(GenUtils.chance(random, 3, 4))
    						break;
    					new DirectionalBuilder(Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL)
    					.setFacing(BlockUtils.getLeft(w.getDirection()))
    					.apply(w);
    					break;
    				case 3:
    					if(GenUtils.chance(random, 1,4))
	    					new ChestBuilder(Material.CHEST)
	    					.setFacing(w.getDirection())
	    					.setLootTable(TerraLootTable.VILLAGE_ARMORER)
	    					.apply(w);
    					break;
    				}
    			}
    			
    			w = w.getLeft();
    		}
    	}
    }
}
