"use strict";

COI.module("Fechamento", function(Module, COI, Backbone, Marionette, $, _) {

	var Saida = Backbone.Model.extend({
		defaults: {
			descricao: null,
			valor: null
		}
	});
	
	var Saidas = Backbone.Collection.extend({
		model: Saida
	});
	
	var Fechamento = Backbone.Model.extend({
		urlRoot: '/rest/fechamentos',
		defaults: function() {
			return {
				data: new Date().getTime(),
				valorDinheiro: 0,
				valorCartao: 0,
				valorCheque: 0,
				saidas: new Saidas()
			};
		},
		parse: function(resp, options) {
			resp.saidas = new Saidas(resp.saidas, {parse: true});
			return resp;
		},
	});
	
	var SaidaView = Marionette.ItemView.extend({
		template: '#fechamento_saidas_row_template',
		tagName: 'tr',
		ui: {
			'descricao': 'input[name=descricao]',
			'valor': 'input[name=valor]',
			'actionRemove': 'button'
		},
		triggers: {
			'click .coi-action-remove': 'remove'
		},
		modelBinder: function() {
			return new Backbone.ModelBinder();
		},
		initialize: function() {
			_.bindAll(this);
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.$el, 'name');
			bindings['valor'].converter = moneyConverter;
			this.modelBinder().bind(this.model, this.$el, bindings);
			
			this.ui.descricao.input();
			this.ui.valor.input();
			this.ui.actionRemove.button();
		},
		onRemove: function(e) {
			this.model.collection.remove(this.model);
		}
	});
	
	var SaidasView = COI.FormCompositeView.extend({
		template: '#fechamento_saidas_template',
		itemView: SaidaView,
		itemViewContainer: 'table',
		ui : {
			'buttonInclude': '.coi-action-include'
		},
		triggers: {
			'click .coi-action-include': 'include'
		},
		initialize: function() {
			_.bindAll(this);
		},
		onRender: function() {
			this.ui.buttonInclude.button();
		},
		onInclude: function(e) {
			this.collection.add(new Saida({
				descricao: null,
				valor: null
			}));
		}
	});
	
	var View = COI.FormView.extend({
		template: '#fechamento_template',
		regions: {
			'saidas': '#saidas'
		},
		modelEvents: {
			'change:saidas': 'renderSaidas'
		},
		initialize: function() {
			_.bindAll(this);
			if (!this.model.isNew()) {
				this.model.fetch();
			}
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
			bindings['data'].converter = timestampConverter;
			bindings['valorDinheiro'].converter = moneyConverter;
			bindings['valorCartao'].converter = moneyConverter;
			bindings['valorCheque'].converter = moneyConverter;
			this.modelBinder().bind(this.model, this.el, bindings);
			
			this.renderSaidas();
		},
		renderSaidas: function() {
			this.saidas.show(new SaidasView({collection: this.model.get('saidas')}));
		},
		onShow: function() {
			this.$el.find('input').first().focus();
		},
		onCancel: function(e) {
			Backbone.history.navigate('fechamentos', true);
		},
		onConfirm: function(e) {
			if (_validate(this)) {
				this.model.save(null, {wait: true, success: this.onComplete, error: this.onError});
			}
		},
		onComplete: function(e) {
			Backbone.history.navigate('fechamentos', true);
			_notifySuccess();
		},
		onError: function(model, resp, options) {
			_notifyError(resp.responseText);
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('fechamento', function() {
			COI.body.show(new View({model: new Fechamento()}));
		});
		COI.router.route('fechamento(/:id)', function(id) {
			COI.body.show(new View({model: new Fechamento({id: id})}));
		});
	});
});