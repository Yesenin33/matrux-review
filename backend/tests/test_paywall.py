def test_pro_image_without_device_402(client, publish):
    publish("debian", pro=True)
    response = client.get("/api/images/debian")
    assert response.status_code == 402
    assert response.json() == {"error": "subscription_required"}


def test_pro_image_free_device_402_but_metadata_listed(client, publish):
    publish("debian", pro=True)
    response = client.get("/api/images/debian", params={"device_id": "free-device"})
    assert response.status_code == 402
    assert response.json() == {"error": "subscription_required"}
    ids = [image["id"] for image in client.get("/api/manifest").json()["images"]]
    assert ids == ["debian"]


def test_pro_image_with_subscription_200(client, publish, subscribe):
    image = publish("debian", pro=True)
    subscribe("paid-device")
    response = client.get("/api/images/debian", params={"device_id": "paid-device"})
    assert response.status_code == 200
    assert response.content == image.data


def test_free_image_needs_no_subscription(client, publish):
    publish("alpine", pro=False)
    assert client.get("/api/images/alpine").status_code == 200


def test_terminated_subscription_loses_downloads(client, publish, subscribe):
    publish("debian", pro=True)
    subscribe("dev-1", "stop-1")
    assert client.get("/api/images/debian", params={"device_id": "dev-1"}).status_code == 402
