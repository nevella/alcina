Changes from gwt2.9:

- optimise codegen for lambdas - they particularly don't need individual type names (instead, reuse the type id) - cuts about 4.5% from final zip