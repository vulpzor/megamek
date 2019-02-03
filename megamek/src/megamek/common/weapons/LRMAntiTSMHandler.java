/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Report;
import megamek.common.Tank;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;
import megamek.server.Server.DamageType;

/**
 * @author Sebastian Brocks
 */
public class LRMAntiTSMHandler extends LRMHandler {

    /**
     *
     */
    private static final long serialVersionUID = 5702089152489814687L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public LRMAntiTSMHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
        sSalvoType = " anti-TSM missile(s) ";
        damageType = DamageType.ANTI_TSM;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            if (ae instanceof BattleArmor) {
                bSalvo = true;
                return ((BattleArmor) ae).getShootingStrength();
            }
            return 1;
        }
        int missilesHit;
        int nMissilesModifier = getClusterModifiers(false);

        boolean bMekTankStealthActive = false;
        if ((ae instanceof Mech) || (ae instanceof Tank)) {
            bMekTankStealthActive = ae.isStealthActive();
        }
        
        // AMS mod
        nMissilesModifier += getAMSHitsMod(vPhaseReport);
        
        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
            Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                    : null;
            if (entityTarget.hasETypeFlag(Entity.ETYPE_DROPSHIP)
                    || entityTarget.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
                nMissilesModifier -= getAeroSanityAMSHitsMod();
            }
        }
        
        if (allShotsHit()) {
            missilesHit = wtype.getRackSize();
        } else {
            // anti tsm hit with half the normal number, round up
            missilesHit = Compute.missilesHit(wtype.getRackSize(),
                    nMissilesModifier, weapon.isHotLoaded(), false, isAdvancedAMS());
            missilesHit = (int) Math.ceil((double) missilesHit / 2);
        }
        Report r = new Report(3325);
        r.subject = subjectId;
        r.add(missilesHit);
        r.add(sSalvoType);
        r.add(toHit.getTableDesc());
        r.newlines = 0;
        vPhaseReport.addElement(r);
        if (bMekTankStealthActive) {
            // stealth prevents bonus
            r = new Report(3335);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        if (nMissilesModifier != 0) {
            if (nMissilesModifier > 0) {
                r = new Report(3340);
            } else {
                r = new Report(3341);
            }
            r.subject = subjectId;
            r.add(nMissilesModifier);
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        r = new Report(3345);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return missilesHit;
    }
}
