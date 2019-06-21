package org.opentosca.planbuilder.model.plan;

public abstract class AbstractActivity {

    private final String id;
    private final ActivityType type;

    public AbstractActivity(final String id, final ActivityType type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return this.id;
    }

    public ActivityType getType() {
        return this.type;
    }

    /**
     * Utils function which returns a suffix depending on the {@link ActivityType}
     *
     * @return Suffix String
     */
    protected static String getIdSuffix(final ActivityType activityType) {
        switch (activityType) {
            case FREEZE:
                return "freeze_activity";
            case MIGRATION:
                return "migration_activity";
            case PROVISIONING:
                return "provisioning_activity";
            case RECURSIVESELECTION:
                return "rescursiveselection_activity";
            case TERMINATION:
                return "termination_activity";
            case STRATEGICSELECTION:
                return "stategicselection_activity";
            case DEFROST:
                return "defrost_activity";
            case NONE:
            default:
                return "none_activity";
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof AbstractActivity)) {
            return false;
        }

        final AbstractActivity act = (AbstractActivity) obj;

        if (!act.getId().equals(this.id)) {
            return false;
        }
        if (!act.getType().equals(getType())) {
            return false;
        }
        return true;
    }

}
