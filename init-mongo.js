db.createUser(
  {
    user: "rodneylai-test",
    pwd: "rodneylai-test",
    roles: [
      {
        role: "readWrite",
        db: "rodneylai-test"
      }
    ]
  }
);
