"use strict";

COI.module("Pagamento", function(Module, COI, Backbone, Marionette, $, _) {

	var Pagamento = Backbone.Model.extend({
		urlRoot: 'rest/pagamentos',
		defaults: {
			categoria: null,
			tipo: 'Saída',
			vencimento: null,
			descricao: null,
			valor: null,
			situacao: 'Pendente',
			pagamento: null,
			formaPagamento: null,
			banco: null,
			agencia: null,
			conta: null,
			cheque: null,
			projecao: 1,
			documento: null
		},
		toJSON: function(options) {
			var attributes = _.clone(this.attributes);
			attributes.vencimento = toTimestamp(attributes.vencimento);
			attributes.pagamento = toTimestamp(attributes.pagamento);
			return attributes;
		}
	});
	
	var CategoriaView = Marionette.Layout.extend({
		template: '#pagamento_categoria_item_template',
		templateHelpers: {
			elementos: []
		},
		ui: {
			'select': 'select'
		},
		modelBinder: function() {
			return new Backbone.ModelBinder();
		},
		serializeData: function() {
			var data = Marionette.ItemView.prototype.serializeData.apply(this, Array.prototype.slice.apply(arguments));
			return _.extend(data, {
				name: this.options.attribute,
				label: this.options.label
			});
		},
		initialize: function() {
			var that = this;
			this.templateHelpers.elementos.length = 0;
			$.get('rest/pagamentos/categorias', function(elementos) {
				$.each(elementos, function(index, element) {
					that.templateHelpers.elementos.push(element.descricao);
				});
				that.render();
			});
		},
		onRender: function() {
			this.modelBinder().bind(this.model, this.el);
			this.ui.select.input();
		},
	});
	
	var View = COI.FormView.extend({
		template: '#pagamento_template',
		regions: {
			'categoria': '#region-categoria'
		},
		ui: {
			'pagamento': '#pagamento',
			'cheque': '#cheque',
			'situacao': '#situacao',
			'formaPagamento': '#formaPagamento',
			'projecao': '#projecao',
			'documento': '#documento'
		},
		triggers: {
			'change #categoria': 'changeCategoria'
		},
		modelEvents: {
			'change:situacao': 'updateSituacao',
			'change:formaPagamento': 'updateFormaPagamento'
		},
		initialize: function() {
			if (!this.model.isNew()) {
				this.model.fetch();
			}
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
			bindings['vencimento'].converter = dateConverter;
			bindings['valor'].converter = moneyConverter;
			bindings['pagamento'].converter = dateConverter;
			this.modelBinder().bind(this.model, this.el, bindings);

			if (!this.model.isNew()) {
				this.ui.projecao.hide();
			}
			
			this.updateSituacao(this.model, this.ui.situacao.val());
			this.updateFormaPagamento(this.model, this.ui.formaPagamento.val());
			this.renderCategoria();
		},
		onShow: function() {
			this.$el.find('input').first().focus();
		},
		renderCategoria: function() {
			this.categoria.show(new CategoriaView({model: this.model, label: 'Categoria:', attribute: 'categoria'}));
		},
		updateSituacao: function(model, value) {
			if ('Pago' == value) {
				this.ui.pagamento.show();
			} else {
				this.ui.pagamento.hide();
			}
		},
		updateFormaPagamento: function(model, value) {
			if ('Cheque' == value) {
				this.ui.cheque.show();
			} else {
				this.ui.cheque.hide();
			}
		},
		onCancel: function(e) {
			Backbone.history.navigate('pagamentos', true);
		},
		onConfirm: function(e) {
			if (_validate(this)) {
				this.model.save(null, {wait: true, success: this.onComplete, error: this.onError});
			}
		},
		onComplete: function(e) {
			Backbone.history.navigate('pagamentos', true);
			_notifySuccess();
		},
		onError: function(model, resp, options) {
			_notifyError(resp.responseText);
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('pagamento', function() {
			COI.body.show(new View({model: new Pagamento()}));
		});
		COI.router.route('pagamento(/:id)', function(id) {
			COI.body.show(new View({model: new Pagamento({id: id})}));
		});
	});
});