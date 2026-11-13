package com.edgareldy.springgraphqltutorial.graphql.input;

import java.util.List;

/**
 * Input for the updateRolePermissions mutation. add and remove carry permission
 * ids to link or unlink from the role. Both fields are optional; at least one
 * must be non-empty and no id may appear in both lists.
 * <p>
 * Created by edgar.muhamyangabo on 11/13/26
 * Author : edgar.muhamyangabo
 * Date : 11/13/26
 * Project : spring-graphql-tutorial
 */
public record UpdateRolePermissionsInput(List<Long> add, List<Long> remove) {
}
