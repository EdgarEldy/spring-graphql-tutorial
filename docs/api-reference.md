# API Reference

Endpoint: `http://localhost:8080/graphql`

All operations are sent as HTTP POST to this single endpoint with a JSON body containing
`query` (the GraphQL document) and optionally `variables` (an object of named variables).

An interactive explorer (GraphiQL) is available at `http://localhost:8080/graphiql`
in the `dev` profile.

## Authentication

Operations that require authentication expect a JWT access token in the `Authorization`
header:

```
Authorization: Bearer <access_token>
```

Obtain a token via the `login` mutation. Access levels in this document:

| Level | Meaning |
|---|---|
| Public | No token required |
| Authenticated | Any valid, non-expired, non-blacklisted token for an enabled, non-locked account |
| ADMIN | Token must carry the `ROLE_ADMIN` authority |

---

## Response format

Successful responses follow the standard GraphQL envelope:

```json
{
  "data": { "operationName": { ... } }
}
```

Errors (not found, forbidden, validation, etc.) are returned in the `errors` array:

```json
{
  "errors": [
    {
      "message": "Product not found with id 99",
      "extensions": {
        "classification": "NOT_FOUND"
      }
    }
  ],
  "data": null
}
```

Error classifications returned by this API:

| Classification | When |
|---|---|
| `NOT_FOUND` | Resource does not exist |
| `BAD_REQUEST` | Validation failure, duplicate name/email, business rule violation (e.g. delete a category that has products) |
| `FORBIDDEN` | Missing token, expired token, insufficient role, account disabled or locked |
| `CONFLICT` | Optimistic locking failure - stale `version` on a product update (reported in `extensions.classification`; the top-level error uses `BAD_REQUEST`) |
| `INTERNAL_ERROR` | Unexpected server error (original message is never exposed) |

---

## Authentication operations

### register

```graphql
mutation ($input: RegisterInput!) {
  register(input: $input) {
    accessToken
    tokenType
    refreshToken
    expiresIn
    user { id firstName lastName email enabled }
  }
}
```

Variables:
```json
{
  "input": {
    "firstName": "Ada",
    "lastName": "Lovelace",
    "email": "ada@example.com",
    "password": "Password1@"
  }
}
```

Access: **Public**. Creates an account with `enabled = false`. Sends an activation email.

### activateAccount

```graphql
mutation ($token: String!) {
  activateAccount(token: $token)
}
```

Access: **Public**. Validates the token and sets `enabled = true`. Returns `true` on success.

### login

```graphql
mutation ($input: LoginInput!) {
  login(input: $input) {
    accessToken
    tokenType
    refreshToken
    expiresIn
    user { id firstName lastName email enabled roles { roleName } }
  }
}
```

Variables:
```json
{
  "input": {
    "email": "ada@example.com",
    "password": "Password1@"
  }
}
```

Access: **Public**. Returns access token (1 hour) and refresh token (7 days).

### refreshToken

```graphql
mutation ($token: String!) {
  refreshToken(token: $token) {
    accessToken
    tokenType
    refreshToken
    expiresIn
  }
}
```

Access: **Public**. Verifies the refresh token and issues a new access and refresh token pair.

### logout

```graphql
mutation {
  logout
}
```

Access: **Authenticated**. Blacklists the current access token by its `jti` claim.

### me

```graphql
{
  me {
    id firstName lastName email enabled accountLocked
    roles { roleName permissions { resource action } }
  }
}
```

Access: **Authenticated**. Returns the current user's profile.

### updateProfile

```graphql
mutation ($input: UpdateProfileInput!) {
  updateProfile(input: $input) {
    id firstName lastName email
  }
}
```

Variables:
```json
{
  "input": { "firstName": "Ada", "lastName": "Lovelace" }
}
```

Access: **Authenticated**. Updates the current user's first name, last name, and/or email.
All fields are optional.

### requestPasswordReset

```graphql
mutation ($email: String!) {
  requestPasswordReset(email: $email)
}
```

Access: **Public**. Sends a password-reset email if the address is known. Always returns
`true` to avoid user enumeration.

### resetPassword

```graphql
mutation ($token: String!, $newPassword: String!) {
  resetPassword(token: $token, newPassword: $newPassword)
}
```

Access: **Public**. Resets the password. The token is single-use and deleted on consumption.

---

## Categories

### categories

```graphql
query ($page: Int, $size: Int) {
  categories(page: $page, size: $size) {
    content { id categoryName createdAt updatedAt }
    totalElements totalPages page size
  }
}
```

Access: **Public**. Returns a paginated list of categories.

### category

```graphql
query ($id: ID!) {
  category(id: $id) {
    id categoryName createdAt updatedAt
  }
}
```

Access: **Public**. Returns a single category or a `NOT_FOUND` error.

### createCategory

```graphql
mutation ($input: CategoryInput!) {
  createCategory(input: $input) {
    id categoryName createdAt updatedAt
  }
}
```

Variables: `{ "input": { "categoryName": "Electronics" } }`

Access: **ADMIN**.

### updateCategory

```graphql
mutation ($id: ID!, $input: CategoryInput!) {
  updateCategory(id: $id, input: $input) {
    id categoryName updatedAt
  }
}
```

Access: **ADMIN**. Returns `NOT_FOUND` if the category does not exist.

### deleteCategory

```graphql
mutation ($id: ID!) {
  deleteCategory(id: $id)
}
```

Access: **ADMIN**. Returns `true` on success. Returns `BAD_REQUEST` if products
reference this category.

---

## Products

### products

```graphql
query ($categoryId: ID, $page: Int, $size: Int) {
  products(categoryId: $categoryId, page: $page, size: $size) {
    content {
      id productName unitPrice version
      category { id categoryName }
    }
    totalElements totalPages page size
  }
}
```

Access: **Public**. `categoryId` is optional; omit it to list all products.

### product

```graphql
query ($id: ID!) {
  product(id: $id) {
    id productName unitPrice version
    category { id categoryName }
  }
}
```

Access: **Public**.

### createProduct

```graphql
mutation ($input: ProductInput!) {
  createProduct(input: $input) {
    id productName unitPrice version
    category { id categoryName }
  }
}
```

Variables: `{ "input": { "productName": "Mechanical Keyboard", "unitPrice": 79.99, "categoryId": "1" } }`

Access: **ADMIN**.

### updateProduct

```graphql
mutation ($id: ID!, $input: ProductInput!) {
  updateProduct(id: $id, input: $input) {
    id productName unitPrice version
  }
}
```

Access: **ADMIN**. Returns `CONFLICT` on a stale `version`.

### deleteProduct

```graphql
mutation ($id: ID!) {
  deleteProduct(id: $id)
}
```

Access: **ADMIN**. Returns `BAD_REQUEST` if orders reference this product.

---

## Customers

### customers

```graphql
query ($page: Int, $size: Int) {
  customers(page: $page, size: $size) {
    content { id firstName lastName telephone email address }
    totalElements totalPages page size
  }
}
```

Access: **Authenticated**.

### customer

```graphql
query ($id: ID!) {
  customer(id: $id) {
    id firstName lastName telephone email address
  }
}
```

Access: **Authenticated**.

### createCustomer

```graphql
mutation ($input: CustomerInput!) {
  createCustomer(input: $input) {
    id firstName lastName telephone email address
  }
}
```

Variables:
```json
{
  "input": {
    "firstName": "Ada", "lastName": "Lovelace",
    "telephone": "+1 202-555-0100",
    "email": "ada@example.com",
    "address": "1 Analytical Engine Way"
  }
}
```

Access: **ADMIN**.

### updateCustomer

```graphql
mutation ($id: ID!, $input: CustomerInput!) {
  updateCustomer(id: $id, input: $input) {
    id firstName lastName email
  }
}
```

Access: **ADMIN**.

### deleteCustomer

```graphql
mutation ($id: ID!) {
  deleteCustomer(id: $id)
}
```

Access: **ADMIN**. Returns `BAD_REQUEST` if orders reference this customer.

---

## Orders

### orders

```graphql
query ($customerId: ID, $page: Int, $size: Int) {
  orders(customerId: $customerId, page: $page, size: $size) {
    content {
      id quantity total
      customer { id firstName lastName email }
      product { id productName unitPrice category { categoryName } }
    }
    totalElements totalPages page size
  }
}
```

Access: **Authenticated**. `customerId` is optional.

### order

```graphql
query ($id: ID!) {
  order(id: $id) {
    id quantity total
    customer { id firstName lastName email }
    product { id productName unitPrice category { categoryName } }
  }
}
```

Access: **Authenticated**.

### createOrder

```graphql
mutation ($input: OrderInput!) {
  createOrder(input: $input) {
    id quantity total
    customer { id firstName lastName }
    product { id productName unitPrice }
  }
}
```

Variables: `{ "input": { "customerId": "1", "productId": "1", "quantity": 2 } }`

Access: **ADMIN**. `total` is computed server-side as `quantity * product.unitPrice`.

---

## Users (ADMIN only)

### users

```graphql
query ($page: Int, $size: Int) {
  users(page: $page, size: $size) {
    content { id firstName lastName email enabled accountLocked roles { roleName } }
    totalElements totalPages page size
  }
}
```

### user

```graphql
query ($id: ID!) {
  user(id: $id) {
    id firstName lastName email enabled accountLocked
    roles { roleName permissions { resource action } }
  }
}
```

### createUser

```graphql
mutation ($input: CreateUserInput!) {
  createUser(input: $input) {
    id firstName lastName email enabled
  }
}
```

Variables:
```json
{
  "input": {
    "firstName": "Grace", "lastName": "Hopper",
    "email": "grace@example.com",
    "password": "Password1@"
  }
}
```

Admin-created accounts are `enabled = true` immediately (no activation email).

### updateUser

```graphql
mutation ($id: ID!, $input: UpdateUserInput!) {
  updateUser(id: $id, input: $input) {
    id firstName lastName email
  }
}
```

Variables:
```json
{
  "id": "2",
  "input": { "firstName": "Grace", "lastName": "Hopper-Updated", "email": "grace2@example.com" }
}
```

### lockUser / unlockUser

```graphql
mutation ($id: ID!) { lockUser(id: $id) { id accountLocked } }
mutation ($id: ID!) { unlockUser(id: $id) { id accountLocked } }
```

### deleteUser

```graphql
mutation ($id: ID!) { deleteUser(id: $id) }
```

### updateUserRoles

```graphql
mutation ($id: ID!, $input: UpdateUserRolesInput!) {
  updateUserRoles(id: $id, input: $input) {
    id roles { roleName }
  }
}
```

Variables:
```json
{
  "id": "2",
  "input": { "add": ["1"], "remove": ["2"] }
}
```

Both `add` and `remove` are optional. Idempotent: adding an already-assigned role or
removing an absent one is a no-op. Returns `NOT_FOUND` if any role ID is unknown.

---

## Roles (ADMIN only)

### roles

```graphql
{ roles { id roleName permissions { id resource action } } }
```

### role

```graphql
query ($id: ID!) {
  role(id: $id) { id roleName permissions { id resource action } }
}
```

### createRole

```graphql
mutation ($input: RoleInput!) {
  createRole(input: $input) { id roleName }
}
```

Variables: `{ "input": { "roleName": "MODERATOR" } }`

### updateRole

```graphql
mutation ($id: ID!, $input: RoleInput!) {
  updateRole(id: $id, input: $input) { id roleName }
}
```

### deleteRole

```graphql
mutation ($id: ID!) { deleteRole(id: $id) }
```

### updateRolePermissions

```graphql
mutation ($roleId: ID!, $input: UpdateRolePermissionsInput!) {
  updateRolePermissions(roleId: $roleId, input: $input) {
    id roleName permissions { id resource action }
  }
}
```

Variables:
```json
{
  "roleId": "1",
  "input": { "add": ["3", "4"], "remove": ["1"] }
}
```

Same idempotent semantics as `updateUserRoles`.

---

## Permissions (ADMIN only)

### permissions

```graphql
{ permissions { id resource action } }
```

### createPermission

```graphql
mutation ($input: PermissionInput!) {
  createPermission(input: $input) { id resource action }
}
```

Variables: `{ "input": { "resource": "PRODUCTS", "action": "WRITE" } }`

The `resource + action` pair must be unique.

### deletePermission

```graphql
mutation ($id: ID!) { deletePermission(id: $id) }
```

---

## Subscription

### orderCreated

```graphql
subscription {
  orderCreated {
    id quantity total
    customer { id firstName lastName }
    product { id productName unitPrice }
  }
}
```

Connect via WebSocket to `ws://localhost:8080/graphql` (the `graphql-ws` subprotocol).
The event fires after each `createOrder` mutation is committed. Access: **Authenticated**.
