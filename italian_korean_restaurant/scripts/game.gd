extends Node3D

const Catalog = preload("res://scripts/recipe_catalog.gd")

var player: CharacterBody3D
var active_recipe: Dictionary = {}
var order_state := "waiting"
var money := 0
var served_count := 0
var prep_time := 0.0
var prep_duration := 1.0
var hud: CanvasLayer
var order_label: Label
var status_label: Label
var money_label: Label
var progress: ProgressBar
var action_button: Button
var customer_nodes: Array[Node3D] = []
var customer_index := 0
var move_vector := Vector2.ZERO
var move_buttons: Dictionary = {}

const MAT_WALL = Color(0.25, 0.19, 0.16)
const MAT_WOOD = Color(0.34, 0.19, 0.11)
const MAT_FLOOR = Color(0.34, 0.29, 0.23)
const MAT_GOLD = Color(0.78, 0.56, 0.25)

func _ready() -> void:
	randomize()
	_build_environment()
	_build_player()
	_build_customers()
	_build_hud()
	_new_order()

func _process(delta: float) -> void:
	if order_state == "preparing":
		prep_time += delta
		progress.value = minf(100.0, prep_time / prep_duration * 100.0)
		if prep_time >= prep_duration:
			order_state = "ready"
			status_label.text = "ORDER READY  •  Take it to the customer"
			action_button.text = "SERVE ORDER"
			progress.value = 100.0

func _physics_process(_delta: float) -> void:
	if not is_instance_valid(player):
		return
	var direction := Vector3(move_vector.x, 0.0, move_vector.y)
	if Input.is_key_pressed(KEY_W): direction.z -= 1.0
	if Input.is_key_pressed(KEY_S): direction.z += 1.0
	if Input.is_key_pressed(KEY_A): direction.x -= 1.0
	if Input.is_key_pressed(KEY_D): direction.x += 1.0
	direction = direction.normalized()
	player.velocity.x = direction.x * 4.0
	player.velocity.z = direction.z * 4.0
	player.move_and_slide()
	player.global_position.x = clampf(player.global_position.x, -8.0, 8.0)
	player.global_position.z = clampf(player.global_position.z, -5.5, 7.0)

func _build_environment() -> void:
	var world_env := WorldEnvironment.new()
	var env := Environment.new()
	env.background_mode = Environment.BG_COLOR
	env.background_color = Color(0.045, 0.055, 0.065)
	env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	env.ambient_light_color = Color(0.72, 0.68, 0.61)
	env.ambient_light_energy = 0.65
	world_env.environment = env
	add_child(world_env)

	var sun := DirectionalLight3D.new()
	sun.rotation_degrees = Vector3(-48, -32, 0)
	sun.light_energy = 1.1
	sun.shadow_enabled = true
	add_child(sun)

	_box("Floor", Vector3(0,-0.2,0), Vector3(19,0.4,16), Color(0.25,0.20,0.16))
	_box("Back wall", Vector3(0,2.1,-7.8), Vector3(19,4.2,0.35), MAT_WALL)
	_box("Left wall", Vector3(-9.4,2.1,0), Vector3(0.35,4.2,16), MAT_WALL)
	_box("Right wall", Vector3(9.4,2.1,0), Vector3(0.35,4.2,16), MAT_WALL)
	_box("Ceiling beam", Vector3(0,4.2,-7.5), Vector3(19,0.2,0.5), MAT_WOOD)

	for x in [-6.0,-2.0,2.0,6.0]:
		_box("Warm ceiling light", Vector3(x,3.85,-1.0), Vector3(1.5,0.12,0.45), Color(0.95,0.72,0.42))
		var lamp := OmniLight3D.new()
		lamp.position = Vector3(x,3.5,-1.0)
		lamp.light_color = Color(1.0,0.72,0.45)
		lamp.light_energy = 1.4
		lamp.omni_range = 7.0
		add_child(lamp)

	# Service counter and distinct kitchen stations.
	_box("Main service counter", Vector3(0,0.65,-4.0), Vector3(8.8,1.3,1.1), MAT_WOOD)
	_box("Counter top", Vector3(0,1.34,-4.0), Vector3(9.0,0.12,1.25), Color(0.17,0.17,0.16))
	var station_names := ["PIZZA OVEN","PASTA RANGE","KOREAN WOK","SOUP & RICE","DESSERT"]
	for i in range(station_names.size()):
		var x := -7.0 + float(i) * 3.5
		_box(station_names[i], Vector3(x,0.7,-6.5), Vector3(2.7,1.4,1.5), Color(0.18,0.20,0.21))
		_box("Steel top", Vector3(x,1.43,-6.5), Vector3(2.8,0.08,1.6), Color(0.48,0.49,0.47))
		_box("Station label", Vector3(x,2.0,-6.5), Vector3(2.4,0.28,0.08), MAT_GOLD)

	# Dining tables, seats and decorative planters.
	for z in [-1.0,3.0]:
		for x in [-6.0,-2.0,2.0,6.0]:
			_box("Dining table", Vector3(x,0.85,z), Vector3(2.0,0.18,1.5), MAT_WOOD)
			for dx in [-0.7,0.7]:
				for dz in [-0.9,0.9]:
					_box("Chair seat", Vector3(x+dx,0.45,z+dz), Vector3(0.65,0.18,0.65), Color(0.22,0.16,0.13))
					_box("Chair back", Vector3(x+dx,0.85,z+dz+0.22), Vector3(0.65,0.8,0.12), Color(0.22,0.16,0.13))
	_box("Window frame", Vector3(-7.0,2.4,-7.58), Vector3(3.0,1.8,0.12), Color(0.12,0.12,0.13))
	_box("Window glass", Vector3(-7.0,2.4,-7.48), Vector3(2.7,1.55,0.04), Color(0.20,0.34,0.42))
	_box("Restaurant sign", Vector3(0,3.2,-7.55), Vector3(4.5,0.7,0.16), MAT_GOLD)

func _build_player() -> void:
	player = CharacterBody3D.new()
	player.name = "Player"
	player.position = Vector3(0,0.1,5.5)
	var capsule := CapsuleMesh.new()
	capsule.radius = 0.34
	capsule.height = 1.65
	var body := MeshInstance3D.new()
	body.mesh = capsule
	body.position.y = 0.85
	body.material_override = _material(Color(0.22,0.25,0.29))
	player.add_child(body)
	var apron := MeshInstance3D.new()
	var apron_mesh := BoxMesh.new()
	apron_mesh.size = Vector3(0.52,0.65,0.12)
	apron.mesh = apron_mesh
	apron.position = Vector3(0,0.82,-0.27)
	apron.material_override = _material(Color(0.12,0.15,0.17))
	player.add_child(apron)
	var collider := CollisionShape3D.new()
	var shape := CapsuleShape3D.new()
	shape.radius = 0.34
	shape.height = 1.65
	collider.shape = shape
	collider.position.y = 0.85
	player.add_child(collider)
	add_child(player)

	var camera := Camera3D.new()
	camera.position = Vector3(12,13,16)
	camera.look_at(Vector3(0,0,0), Vector3.UP)
	camera.current = true
	add_child(camera)

func _build_customers() -> void:
	for i in range(4):
		var customer := Node3D.new()
		customer.name = "Customer_%d" % i
		customer.position = Vector3(-3.0 + i * 2.0,0.0,-2.0)
		var mesh := MeshInstance3D.new()
		var capsule := CapsuleMesh.new()
		capsule.radius = 0.32
		capsule.height = 1.55
		mesh.mesh = capsule
		mesh.position.y = 0.78
		mesh.material_override = _material([Color(0.55,0.28,0.2),Color(0.2,0.38,0.52),Color(0.42,0.3,0.48),Color(0.28,0.42,0.32)][i])
		customer.add_child(mesh)
		var head := MeshInstance3D.new()
		var sphere := SphereMesh.new()
		sphere.radius = 0.23
		sphere.height = 0.46
		head.mesh = sphere
		head.position.y = 1.68
		head.material_override = _material(Color(0.72,0.53,0.39))
		customer.add_child(head)
		add_child(customer)
		customer_nodes.append(customer)

func _build_hud() -> void:
	hud = CanvasLayer.new()
	add_child(hud)
	var panel := PanelContainer.new()
	panel.position = Vector2(18,18)
	panel.size = Vector2(390,210)
	panel.add_theme_stylebox_override("panel", _panel_style(Color(0.055,0.065,0.075,0.94), MAT_GOLD))
	hud.add_child(panel)
	var stack := VBoxContainer.new()
	stack.add_theme_constant_override("separation", 8)
	panel.add_child(stack)

	var title := Label.new()
	title.text = "SEOUL  ×  ROMA     |     RESTAURANT"
	title.add_theme_font_size_override("font_size", 18)
	title.add_theme_color_override("font_color", Color(0.95,0.78,0.49))
	stack.add_child(title)

	order_label = Label.new()
	order_label.text = "CUSTOMER ORDER"
	order_label.add_theme_font_size_override("font_size", 17)
	order_label.add_theme_color_override("font_color", Color.WHITE)
	stack.add_child(order_label)

	status_label = Label.new()
	status_label.text = "Welcome. Take the first order."
	status_label.add_theme_color_override("font_color", Color(0.75,0.82,0.84))
	stack.add_child(status_label)

	progress = ProgressBar.new()
	progress.custom_minimum_size = Vector2(0,14)
	progress.value = 0
	stack.add_child(progress)

	money_label = Label.new()
	money_label.text = "₩ 0     •     € 0     •     Served: 0"
	money_label.add_theme_font_size_override("font_size", 16)
	stack.add_child(money_label)

	action_button = Button.new()
	action_button.text = "TAKE ORDER"
	action_button.custom_minimum_size = Vector2(0,48)
	action_button.pressed.connect(_on_action_pressed)
	stack.add_child(action_button)

	var menu_panel := PanelContainer.new()
	menu_panel.position = Vector2(18,242)
	menu_panel.size = Vector2(390,112)
	menu_panel.add_theme_stylebox_override("panel", _panel_style(Color(0.055,0.065,0.075,0.9), Color(0.4,0.48,0.5)))
	hud.add_child(menu_panel)
	var menu_text := Label.new()
	menu_text.text = "40 RECIPES  •  20 ITALIAN  •  20 KOREAN\nMove around the restaurant with the controls.\nKitchen stations are arranged along the back wall."
	menu_text.add_theme_color_override("font_color", Color(0.82,0.85,0.85))
	menu_text.add_theme_font_size_override("font_size", 14)
	menu_panel.add_child(menu_text)

	_add_move_control("▲", Vector2(1050,520), Vector2(62,62), Vector2(0,-1))
	_add_move_control("▼", Vector2(1050,650), Vector2(62,62), Vector2(0,1))
	_add_move_control("◀", Vector2(980,585), Vector2(62,62), Vector2(-1,0))
	_add_move_control("▶", Vector2(1120,585), Vector2(62,62), Vector2(1,0))

func _add_move_control(label_text: String, pos: Vector2, button_size: Vector2, dir: Vector2) -> void:
	var button := Button.new()
	button.text = label_text
	button.position = pos
	button.size = button_size
	button.modulate = Color(1,1,1,0.82)
	hud.add_child(button)
	button.button_down.connect(func(): move_vector = dir)
	button.button_up.connect(func():
		if move_vector == dir:
			move_vector = Vector2.ZERO
	)
	move_buttons[label_text] = button

func _new_order() -> void:
	active_recipe = Catalog.random_recipe()
	order_state = "waiting"
	prep_time = 0.0
	progress.value = 0
	order_label.text = "ORDER:  " + str(active_recipe.name)
	status_label.text = str(active_recipe.cuisine) + "  •  Station: " + str(active_recipe.station)
	action_button.text = "TAKE ORDER"
	action_button.disabled = false

func _on_action_pressed() -> void:
	match order_state:
		"waiting":
			order_state = "preparing"
			prep_time = 0.0
			prep_duration = float(active_recipe.time)
			status_label.text = "Preparing " + str(active_recipe.name) + "..."
			action_button.text = "COOKING..."
			action_button.disabled = true
		"ready":
			order_state = "served"
			var reward := 18 if active_recipe.cuisine == "ITALIAN" else 18000
			money += reward
			served_count += 1
			money_label.text = "Earnings: " + str(money) + "     •     Served: " + str(served_count)
			if customer_index < customer_nodes.size():
				customer_nodes[customer_index].queue_free()
				customer_index += 1
			if customer_index >= customer_nodes.size():
				status_label.text = "All guests served! New guests are arriving."
				_rebuild_customer_queue()
			_new_order()

func _rebuild_customer_queue() -> void:
	customer_index = 0
	_build_customers()

func _box(object_name: String, pos: Vector3, dimensions: Vector3, color: Color) -> MeshInstance3D:
	var instance := MeshInstance3D.new()
	instance.name = object_name
	var mesh := BoxMesh.new()
	mesh.size = dimensions
	instance.mesh = mesh
	instance.position = pos
	instance.material_override = _material(color)
	add_child(instance)
	return instance

func _material(color: Color) -> StandardMaterial3D:
	var mat := StandardMaterial3D.new()
	mat.albedo_color = color
	mat.roughness = 0.72
	return mat

func _panel_style(fill: Color, border_color: Color) -> StyleBoxFlat:
	var style := StyleBoxFlat.new()
	style.bg_color = fill
	style.border_color = border_color
	style.set_border_width_all(2)
	style.set_corner_radius_all(12)
	style.content_margin_left = 14
	style.content_margin_right = 14
	style.content_margin_top = 12
	style.content_margin_bottom = 12
	return style
