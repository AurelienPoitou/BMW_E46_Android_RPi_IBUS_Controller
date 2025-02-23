sudo cp -r ../etc/systemd/* /etc/systemd/

sudo systemctl enable bluetooth-init.service
sudo systemctl enable connected_car_controller.service